/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.fabric.Environment
import com.demonwav.mcdev.platform.fabric.FabricProjectConfiguration
import com.demonwav.mcdev.util.firstOfType
import com.extracraftx.minecraft.templatemakerfabric.data.DataProvider
import com.extracraftx.minecraft.templatemakerfabric.data.holders.LoomVersion
import com.intellij.ui.CollectionComboBoxModel
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.apache.commons.lang.WordUtils
import java.awt.event.ActionListener
import java.io.IOException
import javax.swing.*

class FabricProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    private lateinit var modNameField: JTextField
    private lateinit var modVersionField: JTextField
    private lateinit var mainEntryPointField: JTextField
    private lateinit var clientEntryPointField: JTextField
    private lateinit var panel: JPanel
    private lateinit var title: JLabel
    private lateinit var descriptionField: JTextField
    private lateinit var authorsField: JTextField
    private lateinit var websiteField: JTextField
    private lateinit var repositoryField: JTextField
    private lateinit var minecraftVersionBox: JComboBox<String>
    private lateinit var loaderVersionBox: JComboBox<String>
    private lateinit var yarnVersionBox: JComboBox<String>
    private lateinit var loomVersionBox: JComboBox<String>
    private lateinit var useFabricApiCheckbox: JCheckBox
    private lateinit var fabricApiBox: JComboBox<String>
    private lateinit var environmentBox: JComboBox<String>
    private lateinit var mixinsCheckbox: JCheckBox
    private lateinit var loadingBar: JProgressBar
    private lateinit var minecraftVersionLabel: JLabel
    private lateinit var yarnWarning: JLabel
    private lateinit var errorLabel: JLabel

    private var config: FabricProjectConfiguration? = null

    private var dataProvider: DataProvider? = null

    private var currentJob: Job? = null

    private val yarnBoxActionListener = ActionListener {
        val selectedVersion = yarnVersionBox.selectedItem as? String ?: return@ActionListener
        yarnWarning.isVisible = (dataProvider
                ?.yarnVersions
                ?.firstOrNull { it.name == selectedVersion }
                ?.mcVersion != minecraftVersionBox.selectedItem)
    }

    private val minecraftBoxActionListener: ActionListener = ActionListener {
        CoroutineScope(Dispatchers.Swing).launch {
            loadingBar.isIndeterminate = true
            loadingBar.isVisible = true

            updateForm()

            loadingBar.isIndeterminate = false
            loadingBar.isVisible = false
        }
    }

    init {
        yarnWarning.isVisible = false
    }

    override fun getComponent(): JComponent {
        return panel
    }

    override fun updateStep() {
        config = creator.configs.firstOfType()

        val buildSystem = creator.buildSystem ?: return

        modNameField.text = WordUtils.capitalize(buildSystem.artifactId.replace('-', ' '))
        modVersionField.text = buildSystem.version

        val conf = config ?: return

        if (creator.configs.indexOf(conf) != 0) {
            modNameField.isEditable = false
            modVersionField.isEditable = false
        }

        mainEntryPointField.text = buildSystem.groupId.replace("-", "").toLowerCase() + "." +
                buildSystem.artifactId.replace("-", "").toLowerCase() + "." +
                WordUtils.capitalize(buildSystem.artifactId.replace('-', ' ')).replace(" ", "")

        if (creator.configs.size > 1) {
            mainEntryPointField.text = mainEntryPointField.text + PlatformType.FABRIC.normalName
        }

        title.icon = PlatformAssets.FABRIC_ICON_2X
        title.text = "<html><font size=\"5\">Fabric Settings</font></html>"

        minecraftVersionLabel.text = "Minecraft Version"

        if (dataProvider != null || currentJob?.isActive == true) {
            return
        }
        currentJob = updateVersions()
    }

    private val mcVersion: String?
        get() = minecraftVersionBox.selectedItem as? String

    private val yarnVersion: String?
        get() = yarnVersionBox.selectedItem as? String

    private val loomVersion: String?
        get() = loomVersionBox.selectedItem as? String

    private val loaderVersion: String?
        get() = loaderVersionBox.selectedItem as? String

    private val fabricApiVersion: String?
        get() = fabricApiBox.selectedItem as? String

    private val LoomVersion.simpleName: String
        get() = this.name.substringBefore('+')

    override fun validate(): Boolean {
        return validate(
                modNameField,
                modVersionField,
                mainEntryPointField,
                authorsField,
                null,
                pattern
        ) && !loadingBar.isVisible
    }

    override fun isStepVisible(): Boolean {
        return creator.configs.any { it is FabricProjectConfiguration }
    }

    override fun onStepLeaving() {
        currentJob?.let { job ->
            // we're in a cancel state
            job.cancel()
            return
        }

        val conf = config ?: return
        conf.base = ProjectConfiguration.BaseConfigs(
                pluginName = modNameField.text,
                pluginVersion = modVersionField.text,
                mainClass = mainEntryPointField.text,
                description = descriptionField.text,
                website = websiteField.text
        )

        conf.setAuthors(authorsField.text)
        conf.modRepo = repositoryField.text

        conf.yarnVersion = yarnVersion ?: ""
        conf.mcVersion = mcVersion ?: ""
        val loaderVer = loaderVersion
        if (loaderVer != null)
            conf.loaderVersion = loaderVer
        conf.apiVersion = if (useFabricApiCheckbox.isSelected) dataProvider?.fabricApiVersions?.firstOrNull { it.name == fabricApiVersion }?.mavenVersion else null
        conf.environment = Environment.byName(environmentBox.selectedItem as? String) ?: Environment.BOTH
        conf.mainClass = mainEntryPointField.text.let { if (it.isEmpty()) null else it }
        conf.clientClass = clientEntryPointField.text.let { if (it.isEmpty()) null else it }
        conf.mixins = mixinsCheckbox.isSelected
    }

    fun error() {
        errorLabel.isVisible = true
        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false
    }

    override fun updateDataModel() {}

    private fun updateVersions() = CoroutineScope(Dispatchers.Swing).launch {
        loadingBar.isIndeterminate = true
        loadingBar.isVisible = true

        try {
            dataProvider = downloadVersions()
            updateForm()
        } catch (e: Exception) {
            error()
        }

        loadingBar.isIndeterminate = false
        loadingBar.isVisible = false

        currentJob = null
    }

    private suspend fun downloadVersions(): DataProvider? = coroutineScope {
        val dataProvider = DataProvider()
        val minecraftVersionJob = async(Dispatchers.IO) { try { dataProvider.minecraftVersions } catch (e: IOException) { null } }
        val fabricApiVersionJob = async(Dispatchers.IO) { try { dataProvider.fabricApiVersions } catch (e: IOException) { null } }
        val yarnVersionJob = async(Dispatchers.IO) { try { dataProvider.yarnVersions } catch (e: IOException) { null } }
        val loomVersionJob = async(Dispatchers.IO) { try { dataProvider.loomVersions } catch (e: IOException) { null } }
        val loaderVersionJob = async(Dispatchers.IO) { try { dataProvider.loaderVersions } catch (e: IOException) { null } }

        minecraftVersionJob.await() ?: return@coroutineScope null
        fabricApiVersionJob.await() ?: return@coroutineScope null
        yarnVersionJob.await() ?: return@coroutineScope null
        loomVersionJob.await() ?: return@coroutineScope null
        loaderVersionJob.await() ?: return@coroutineScope null

        return@coroutineScope dataProvider
    }

    private fun updateForm() {
        val dp = dataProvider ?: return

        val mcVer = when {
            mcVersion != null -> {
                mcVersion ?: return
            }
            else -> {
                dp.minecraftVersions.firstOrNull { it.stable }?.name
            }
        }
        val mcVerObj = dp.minecraftVersions.firstOrNull { it.name == mcVer }

        val yarnVer = when {
            yarnVersion != null -> {
                yarnVersion ?: return
            }
            else -> {
                mcVerObj?.let { mvo ->
                    dp.getFilteredYarnVersions(mvo).firstOrNull()?.name
                }
            }
        }
        val yarnVerObj = dp.yarnVersions.firstOrNull { it.name == yarnVer }

        val loomVer = when {
            loomVersion != null -> {
                loomVersion ?: return
            }
            else -> {
                yarnVerObj?.let { dp.getDefaultLoomVersion(it) }?.simpleName
            }
        }
        val loomVerObj = dp.loomVersions.firstOrNull { it.simpleName == loomVer }

        val loaderVer = when {
            loaderVersion != null -> {
                loaderVersion ?: return
            }
            else -> {
                loomVerObj?.let { lvo ->
                    dp.getFilteredLoaderVersions(lvo).firstOrNull()?.name
                }
            }
        }

        val fabricVer = when {
            fabricApiVersion != null -> {
                fabricApiVersion ?: return
            }
            else -> {
                mcVerObj?.let { mvo ->
                    dp.getDefaultFabricApiVersion(mvo)
                }?.let { dp.fabricApiVersions[it] }?.name
            }
        }

        minecraftVersionBox.model = CollectionComboBoxModel(dp.minecraftVersions.map { it.name })
        minecraftVersionBox.selectedItem = mcVer
        yarnVersionBox.model = CollectionComboBoxModel(dp.yarnVersions.map { it.name })
        yarnVersionBox.selectedItem = yarnVer
        loomVersionBox.model = CollectionComboBoxModel(dp.loomVersions.map { it.simpleName })
        loomVersionBox.selectedItem = loomVer
        loaderVersionBox.model = CollectionComboBoxModel(dp.loaderVersions.map { it.name })
        loaderVersionBox.selectedItem = loaderVer
        fabricApiBox.model = CollectionComboBoxModel(dp.fabricApiVersions.map { it.name })
        fabricApiBox.selectedItem = fabricVer
        useFabricApiCheckbox.isSelected = fabricVer != null
    }
}
