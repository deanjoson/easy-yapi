package com.itangcent.idea.plugin.config

import com.google.inject.Inject
import com.google.inject.name.Named
import com.intellij.ide.util.PropertiesComponent
import com.itangcent.common.utils.invokeMethod
import com.itangcent.idea.plugin.settings.SettingBinder
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.config.MutableConfigReader
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.psi.ContextSwitchListener
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.TimeUnit


class RecommendConfigReader : ConfigReader {

    @Inject
    @Named("delegate_config_reader")
    val configReader: ConfigReader? = null

    @Inject(optional = true)
    val settingBinder: SettingBinder? = null

    @Inject
    val logger: Logger? = null

    @Volatile
    private var loading: Boolean = false

    override fun first(key: String): String? {
        checkStatus()
        return configReader!!.first(key)
    }

    override fun foreach(keyFilter: (String) -> Boolean, action: (String, String) -> Unit) {
        checkStatus()
        configReader!!.foreach(keyFilter, action)
    }

    override fun foreach(action: (String, String) -> Unit) {
        checkStatus()
        configReader!!.foreach(action)
    }

    override fun read(key: String): Collection<String>? {
        checkStatus()
        return configReader!!.read(key)
    }

    private fun checkStatus() {
        while (loading) {
            TimeUnit.MILLISECONDS.sleep(100)
        }
    }

    @PostConstruct
    fun init() {

        if (configReader is MutableConfigReader) {
            val contextSwitchListener = ActionContext.getContext()!!.instance(ContextSwitchListener::class)
            contextSwitchListener.clear()
            contextSwitchListener.onModuleChange { module ->
                synchronized(this)
                {
                    loading = true
                    configReader.reset()
                    val moduleFile = module.moduleFile
                    val modulePath: String = when {
                        moduleFile == null -> module.moduleFilePath.substringBeforeLast(File.separator)
                        moduleFile.isDirectory -> moduleFile.path
                        else -> moduleFile.parent.path
                    }
                    configReader.put("module_path", modulePath)
                    initDelegateAndRecommed()
                    loading = false
                }
            }
        } else {
            initDelegateAndRecommed()
        }
    }

    private fun initDelegateAndRecommed() {
        try {
            configReader?.invokeMethod("init")
        } catch (e: Throwable) {
        }
        if (settingBinder?.read()?.useRecommendConfig == true) {
            if (settingBinder.read().recommendConfigs.isEmpty()) {
                logger!!.info(
                    "Even useRecommendConfig was true, but no recommend config be selected!\n" +
                            "\n" +
                            "If you need to enable the built-in recommended configuration." +
                            "Go to [Preference -> Other Setting -> EasyApi -> Recommend]"
                )
                return
            }

            if (configReader is MutableConfigReader) {
                val recommendConfig = buildRecommendConfig(settingBinder.read().recommendConfigs.split(","))

                if (recommendConfig.isEmpty()) {
                    logger!!.warn("No recommend config be selected!")
                    return
                }

                configReader.loadConfigInfoContent(recommendConfig)
                logger!!.info("use recommend config")
            } else {
                logger!!.warn("failed to use recommend config")
            }
        }
    }

    companion object {

        init {
            loadRecommendConfig()
            resolveRecommendConfig(RECOMMEND_CONFIG_PLAINT)
        }

        private const val config_name = ".recommend.easy.api.config"
        //        private const val config_version = ".recommend.easy.api.config.version"
        private const val curr_version = "0.0.6.1"
        //$version$content

        private fun loadRecommendConfig(): String {

            val propertiesComponent = PropertiesComponent.getInstance()
            val cachedValue = propertiesComponent.getValue(config_name)
            if (!cachedValue.isNullOrBlank()) {
                val cachedVersion = cachedValue.substring(0, 10).trim()
                if (cachedVersion == curr_version) {
                    RECOMMEND_CONFIG_PLAINT = cachedValue.substring(10)
                    return cachedValue
                }
            }

            val bufferedReader = BufferedReader(
                InputStreamReader(
                    RecommendConfigReader::class.java.classLoader.getResourceAsStream(config_name)
                        ?: RecommendConfigReader::class.java.getResourceAsStream(config_name)
                )
            )

            val config = bufferedReader.readText()
            RECOMMEND_CONFIG_PLAINT = config
            //the version always take 10 chars
            propertiesComponent.setValue(config_name, curr_version.padEnd(10) + config)
            return config
        }

        private fun resolveRecommendConfig(config: String) {
            val recommendConfig: MutableMap<String, String> = LinkedHashMap()
            val recommendConfigCodes: MutableList<String> = LinkedList()
            var code: String? = null
            var content: String? = ""
            for (line in config.lines()) {
                if (line.startsWith("#[")) {
                    if (code != null) {
                        recommendConfigCodes.add(code)
                        recommendConfig[code] = content ?: ""
                        content = ""
                    }
                    code = line.removeSurrounding("#[", "]")
                } else {
                    if (content.isNullOrBlank()) {
                        content = line
                    } else {
                        content += "\n"
                        content += line
                    }
                }
            }

            if (code != null) {
                recommendConfigCodes.add(code)
                recommendConfig[code] = content ?: ""
            }

            RECOMMEND_CONFIG_CODES = recommendConfigCodes.toTypedArray()
            RECOMMEND_CONFIG_MAP = recommendConfig
        }

        fun buildRecommendConfig(codes: List<String>): String {
            return RECOMMEND_CONFIG_CODES
                .filter { codes.contains(it) }
                .map { RECOMMEND_CONFIG_MAP[it] }
                .joinToString("\n")

        }

        lateinit var RECOMMEND_CONFIG_PLAINT: String
        lateinit var RECOMMEND_CONFIG_CODES: Array<String>
        lateinit var RECOMMEND_CONFIG_MAP: Map<String, String>
    }
}