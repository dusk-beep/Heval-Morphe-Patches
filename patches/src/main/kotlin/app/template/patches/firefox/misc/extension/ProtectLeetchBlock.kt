package app.template.patches.firefox.misc.extension

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val protectLeechBlockPatch = bytecodePatch(
    name = "Protect LeechBlock",
    description = "Prevent LeechBlock from being disabled or uninstalled.",
) {
    execute {
        val classDef = classDefBy(
            "Lmozilla/components/browser/engine/gecko/GeckoEngine;"
        )

        val targetClass = mutableClassDefBy(classDef)

        val disableMethod = targetClass.methods.firstOrNull {
            it.name == "disableWebExtension" &&
                it.parameterTypes.size == 4 &&
                it.returnType == "V"
        } ?: throw PatchException("disableWebExtension not found")

        val uninstallMethod = targetClass.methods.firstOrNull {
            it.name == "uninstallWebExtension" &&
                it.parameterTypes.size == 3 &&
                it.returnType == "V"
        } ?: throw PatchException("uninstallWebExtension not found")

        // Add two local registers to each method.
        disableMethod.implementation?.let {
            it.registerCount += 2
        } ?: throw PatchException("disableWebExtension has no implementation")

        uninstallMethod.implementation?.let {
            it.registerCount += 2
        } ?: throw PatchException("uninstallWebExtension has no implementation")

        // Block disabling LeechBlock.
        disableMethod.addInstructionsWithLabels(
            0,
            """
                iget-object v0, p1, Lmozilla/components/concept/engine/webextension/WebExtension;->id:Ljava/lang/String;
                const-string v1, "leechblockng@proginosko.com"
                invoke-virtual { v1, v0 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v2
                if-eqz v2, :leechblock_disable_continue

                new-instance v0, Ljava/lang/UnsupportedOperationException;
                const-string v1, "LeechBlock is protected"
                invoke-direct { v0, v1 }, Ljava/lang/UnsupportedOperationException;-><init>(Ljava/lang/String;)V
                invoke-virtual { p4, v0 }, Lmozilla/components/feature/addons/AddonManager$$ExternalSyntheticLambda5;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
                return-void

                :leechblock_disable_continue
            """
        )

        // Block uninstalling LeechBlock.
        uninstallMethod.addInstructionsWithLabels(
            0,
            """
                iget-object v0, p1, Lmozilla/components/concept/engine/webextension/WebExtension;->id:Ljava/lang/String;
                const-string v1, "leechblockng@proginosko.com"
                invoke-virtual { v1, v0 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v2
                if-eqz v2, :leechblock_uninstall_continue

                new-instance v2, Ljava/lang/UnsupportedOperationException;
                const-string v1, "LeechBlock is protected"
                invoke-direct { v2, v1 }, Ljava/lang/UnsupportedOperationException;-><init>(Ljava/lang/String;)V
                invoke-virtual { p3, v0, v2 }, Lmozilla/components/feature/addons/AddonManager$$ExternalSyntheticLambda11;->invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                return-void

                :leechblock_uninstall_continue
            """
        )
    }
}
