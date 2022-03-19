package com.github.cherrydevbomb.collabo.services

import com.intellij.openapi.project.Project
import com.github.cherrydevbomb.collabo.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
