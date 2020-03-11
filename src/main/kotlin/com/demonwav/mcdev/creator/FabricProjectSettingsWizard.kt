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
import com.demonwav.mcdev.platform.fabric.EntryPoint
import com.demonwav.mcdev.platform.fabric.Environment
import com.demonwav.mcdev.platform.fabric.FabricConstants
import com.demonwav.mcdev.platform.fabric.FabricProjectConfiguration
import com.demonwav.mcdev.util.firstOfType
import com.extracraftx.minecraft.templatemakerfabric.data.DataProvider
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.EditableModel
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.apache.commons.lang.WordUtils
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.IOException
import java.util.*
import javax.swing.*
import javax.swing.table.AbstractTableModel

class FabricProjectSettingsWizard(private val creator: MinecraftProjectCreator) : MinecraftModuleWizardStep() {

    // Initialize ALL custom fields in createUIComponents, otherwise they are null until after that point!
    private lateinit var modNameField: JTextField
    private lateinit var modVersionField: JTextField
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
    private lateinit var decompileMcCheckbox: JCheckBox
    private lateinit var loadingBar: JProgressBar
    private lateinit var minecraftVersionLabel: JLabel
    private lateinit var entryPointsTable: JPanel
    private lateinit var entryPoints : ArrayList<EntryPoint>
    private lateinit var tableModel : EntryPointTableModel
    private lateinit var yarnWarning: JLabel
    private lateinit var errorLabel: JLabel

    private var config: FabricProjectConfiguration? = null

    private var dataProvider: DataProvider? = null

    private var currentJob: Job? = null

    private val minecraftBoxActionListener: ActionListener = ActionListener {
        yarnVersionBox.selectedItem = null
        loaderVersionBox.selectedItem = null
        loomVersionBox.selectedItem = null
        fabricApiBox.selectedItem = null
        updateForm()
    }

    init {
        yarnWarning.isVisible = false
    }

    fun createUIComponents() {
        entryPoints = arrayListOf()
        tableModel = EntryPointTableModel(entryPoints)
        val entryPointsTable = JBTable(tableModel)
        fun resizeColumns() {
            val model = entryPointsTable.columnModel
            val totalWidth = model.totalColumnWidth
            model.getColumn(0).preferredWidth = (totalWidth * 0.2).toInt()
            model.getColumn(1).preferredWidth = (totalWidth * 0.4).toInt()
            model.getColumn(2).preferredWidth = (totalWidth * 0.4).toInt()
        }
        resizeColumns()
        entryPointsTable.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                resizeColumns()
            }
        })
        this.entryPointsTable = ToolbarDecorator.createDecorator(entryPointsTable).createPanel()
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

        val packageName = "${buildSystem.groupId.replace("-", "").toLowerCase()}.${buildSystem.artifactId.replace("-", "").toLowerCase()}"
        var className = buildSystem.artifactId.replace('-', ' ').let { WordUtils.capitalize(it) }.replace(" ", "")
        if (creator.configs.size > 1)
            className += PlatformType.FABRIC.normalName
        entryPoints.add(EntryPoint("main", "$packageName.$className", FabricConstants.MOD_INITIALIZER))
        entryPoints.add(EntryPoint("client", "", FabricConstants.CLIENT_MOD_INITIALIZER))
        tableModel.fireTableDataChanged()
        entryPointsTable.revalidate()

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

    override fun validate(): Boolean {
        return validate(
                modNameField,
                modVersionField,
                null,
                null, // don't validate authors, always valid
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
                mainClass = "",
                description = descriptionField.text,
                website = websiteField.text
        )

        conf.setAuthors(authorsField.text)
        conf.modRepo = repositoryField.text

        conf.yarnVersion = yarnVersion ?: ""
        conf.yarnClassifier = if (dataProvider?.yarnVersions?.firstOrNull { it.name == yarnVersion }?.hasV2Mappings == false) null else "v2"
        conf.mcVersion = mcVersion ?: ""
        conf.normalizedMcVersion = dataProvider?.getNormalizedMinecraftVersion(mcVersion)?.normalized
        val loaderVer = loaderVersion
        if (loaderVer != null)
            conf.loaderVersion = loaderVer
        val api = if (useFabricApiCheckbox.isSelected) dataProvider?.fabricApiVersions?.firstOrNull { it.name == fabricApiVersion } else null
        conf.apiVersion = api?.mavenVersion
        conf.apiMavenLocation = api?.mavenLocation
        conf.gradleVersion = when (dataProvider?.loomVersions?.firstOrNull { it.name == loomVersion }?.gradle) {
            4 -> "4.10.3"
            else -> "5.5.1"
        }
        val loomVer = loomVersion
        if (loomVer != null)
            conf.gradleLoomVersion = loomVer
        conf.environment = Environment.byName(environmentBox.selectedItem as? String) ?: Environment.BOTH
        conf.entryPoints = entryPoints.filter { it.valid }
        conf.mixins = mixinsCheckbox.isSelected
        conf.genSources = decompileMcCheckbox.isSelected
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
                yarnVerObj?.let { dp.getDefaultLoomVersion(it) }?.name
            }
        }
        val loomVerObj = dp.loomVersions.firstOrNull { it.name == loomVer }

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
                }?.let { dp.sortedFabricApiVersions[it] }?.name
            }
        }

        minecraftVersionBox.removeActionListener(minecraftBoxActionListener)
        minecraftVersionBox.model = CollectionComboBoxModel(dp.minecraftVersions.map { it.name })
        minecraftVersionBox.selectedItem = mcVer
        minecraftVersionBox.addActionListener(minecraftBoxActionListener)
        yarnVersionBox.model = CollectionComboBoxModel(dp.yarnVersions.map { it.name })
        yarnVersionBox.selectedItem = yarnVer
        loomVersionBox.model = CollectionComboBoxModel(dp.loomVersions.map { it.name })
        loomVersionBox.selectedItem = loomVer
        loaderVersionBox.model = CollectionComboBoxModel(dp.loaderVersions.map { it.name })
        loaderVersionBox.selectedItem = loaderVer
        fabricApiBox.model = CollectionComboBoxModel(dp.fabricApiVersions.map { it.name })
        fabricApiBox.selectedItem = fabricVer
        useFabricApiCheckbox.isSelected = fabricVer != null
    }

    class EntryPointTableModel(private val entryPoints: ArrayList<EntryPoint>) : AbstractTableModel(), EditableModel {

        override fun getColumnName(col: Int) = when (col) {
            0 -> "Name"
            1 -> "Class"
            else -> "Interfaces"
        }

        override fun getRowCount() = entryPoints.size

        override fun getColumnCount() = 3

        override fun getValueAt(row: Int, col: Int) = when (col) {
            0 -> entryPoints[row].name
            1 -> entryPoints[row].clazz
            else -> entryPoints[row].interfaces
        }

        override fun isCellEditable(row: Int, col: Int) = true

        override fun setValueAt(value: Any?, row: Int, col: Int) {
            when (col) {
                0 -> entryPoints[row].name = value.toString()
                1 -> entryPoints[row].clazz = value.toString()
                2 -> entryPoints[row].interfaces = value.toString()
            }
            fireTableCellUpdated(row, col)
        }

        override fun removeRow(idx: Int) {
            entryPoints.removeAt(idx)
        }

        override fun exchangeRows(oldIndex: Int, newIndex: Int) {
            Collections.swap(entryPoints, oldIndex, newIndex)
        }

        override fun canExchangeRows(oldIndex: Int, newIndex: Int) = true

        override fun addRow() {
            entryPoints.add(EntryPoint("", "", ""))
        }
    }

}
