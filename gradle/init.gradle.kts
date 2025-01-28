import com.diffplug.gradle.spotless.SpotlessExtension

initscript {
    subprojects {
        plugins.apply(rootProject.libs.plugins.spotless.get().pluginId)
        configure<SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("**/camera/viewfinder/**")
                ktlint(libs.ktlint.get().version)
            }
            kotlinGradle {
                ktlint(libs.ktlint.get().version)
            }
        }
    }
}
