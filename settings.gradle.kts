pluginManagement {
    repositories {
        //mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Moocow9m/SimpleAPIPlugin/")
            /*credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }*/
        }
        gradlePluginPortal()
    }
}

rootProject.name = "SimpleAPIPlugin"

include("plugin")
