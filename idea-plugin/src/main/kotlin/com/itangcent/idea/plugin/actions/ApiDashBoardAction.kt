package com.itangcent.idea.plugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.itangcent.common.exporter.ClassExporter
import com.itangcent.common.exporter.RequestHelper
import com.itangcent.idea.plugin.api.cache.CachedClassExporter
import com.itangcent.idea.plugin.api.dashboard.ApiDashBoard
import com.itangcent.idea.plugin.api.export.DefaultRequestHelper
import com.itangcent.idea.plugin.api.export.SpringClassExporter
import com.itangcent.idea.plugin.api.export.postman.PostmanConfigReader
import com.itangcent.idea.plugin.config.RecommendConfigReader
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.singleton
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.file.DefaultLocalFileRepository
import com.itangcent.intellij.file.LocalFileRepository
import com.itangcent.suv.http.ConfigurableHttpClientProvider
import com.itangcent.suv.http.HttpClientProvider

class ApiDashBoardAction : ApiExportAction("ApiDashBoard") {

    override fun afterBuildActionContext(event: AnActionEvent, builder: ActionContext.ActionContextBuilder) {
        super.afterBuildActionContext(event, builder)

        builder.bind(LocalFileRepository::class) { it.with(DefaultLocalFileRepository::class).singleton() }

        //allow cache api
        builder.bind(ClassExporter::class, "delegate_classExporter") { it.with(SpringClassExporter::class).singleton() }
        builder.bind(ClassExporter::class) { it.with(CachedClassExporter::class).singleton() }

        builder.bind(RequestHelper::class) { it.with(DefaultRequestHelper::class).singleton() }
        builder.bind(ConfigReader::class, "delegate_config_reader") { it.with(PostmanConfigReader::class).singleton() }
        builder.bind(ConfigReader::class) { it.with(RecommendConfigReader::class).singleton() }
        builder.bind(HttpClientProvider::class) { it.with(ConfigurableHttpClientProvider::class).singleton() }


    }

    override fun actionPerformed(actionContext: ActionContext, project: Project?, anActionEvent: AnActionEvent) {
        super.actionPerformed(actionContext, project, anActionEvent)
        val apiDashBoard = actionContext.instance(ApiDashBoard::class)
        apiDashBoard.showDashBoardWindow()
    }
}

