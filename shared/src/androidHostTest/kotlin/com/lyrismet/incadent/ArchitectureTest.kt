package com.lyrismet.incadent

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

private const val BASE = "com.lyrismet.incadent"

/** the presentation subpackages a feature offers to the others - everything else in `presentation` is its own screens */
private val SHARED_PRESENTATION: Map<String, Set<String>> =
    mapOf(
        "codex" to setOf("summary", "quickedit", "style", "form"),
        "campaign" to setOf("sheet"),
        // navigation keys other screens open
        "session" to setOf("route"),
        // every file of these two is meant to be embedded
        "mention" to setOf("*"),
        "phrases" to setOf("*"),
    )

private class SourceFile(
    val file: File,
    val pkg: List<String>,
    val imports: List<List<String>>,
) {
    val layer: String? get() = if (pkg.firstOrNull() == "feature") pkg.getOrNull(2) else null
    val feature: String? get() = if (pkg.firstOrNull() == "feature") pkg.getOrNull(1) else null
    val name: String get() = file.name
}

private fun sourceRoot(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        val candidate = File(dir, "shared/src/commonMain/kotlin/${BASE.replace('.', '/')}")
        if (candidate.isDirectory) return candidate
        val direct = File(dir, "src/commonMain/kotlin/${BASE.replace('.', '/')}")
        if (direct.isDirectory) return direct
        dir = dir.parentFile
    }
    error("commonMain sources not found from ${File("").absolutePath}")
}

private fun sources(): List<SourceFile> =
    sourceRoot()
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .map { file ->
            val text = file.readText()
            val pkg =
                Regex("^package\\s+([\\w.]+)", RegexOption.MULTILINE)
                    .find(text)!!
                    .groupValues[1]
                    .removePrefix(BASE)
                    .removePrefix(".")
                    .split('.')
                    .filter { it.isNotEmpty() }
            val imports =
                Regex("^import\\s+$BASE\\.([\\w.]+)", RegexOption.MULTILINE)
                    .findAll(text)
                    .map { it.groupValues[1].split('.') }
                    .toList()
            SourceFile(file, pkg, imports)
        }.toList()

private fun featureDirs(
    feature: String,
    layer: String,
): Set<String> =
    File(sourceRoot(), "feature/$feature/$layer")
        .listFiles { f -> f.isDirectory }
        .orEmpty()
        .map { it.name }
        .toSet()

/** the dependency rules of the feature-first layout, see "Architecture" in CLAUDE.md */
class ArchitectureTest {
    private val all = sources()

    private fun assertNoViolations(
        rule: String,
        violations: List<String>,
    ) {
        assertTrue(violations.isEmpty(), "$rule\n" + violations.sorted().joinToString("\n"))
    }

    private fun violations(check: (SourceFile, List<String>) -> Boolean): List<String> =
        all.flatMap { source ->
            source.imports
                .filter { check(source, it) }
                .map { "${source.name} (${source.pkg.joinToString(".")}) imports ${it.joinToString(".")}" }
        }

    @Test
    fun `core never knows a feature`() =
        assertNoViolations(
            "core must not import feature, persistence or di",
            violations { source, import ->
                source.pkg.firstOrNull() == "core" && import.first() in setOf("feature", "persistence", "di")
            },
        )

    @Test
    fun `a domain package imports no data or presentation`() =
        assertNoViolations(
            "feature domain must not import data, presentation, persistence or di",
            violations { source, import ->
                source.layer == "domain" &&
                    (
                        (import.first() == "feature" && import.getOrNull(2) in setOf("data", "presentation")) ||
                            import.first() in setOf("persistence", "di")
                    )
            },
        )

    @Test
    fun `a data package stays inside its own feature`() =
        assertNoViolations(
            "feature data must not import another feature's data or any presentation",
            violations { source, import ->
                source.layer == "data" &&
                    import.first() == "feature" &&
                    (
                        import.getOrNull(2) == "presentation" ||
                            (import.getOrNull(2) == "data" && import.getOrNull(1) != source.feature)
                    )
            },
        )

    @Test
    fun `presentation never reaches into any data package`() =
        assertNoViolations(
            "presentation must go through domain interfaces, not data, persistence or di",
            violations { source, import ->
                source.layer == "presentation" &&
                    (
                        (import.first() == "feature" && import.getOrNull(2) == "data") ||
                            import.first() in setOf("persistence", "di")
                    )
            },
        )

    @Test
    fun `another feature's presentation is used only through its shared packages`() =
        assertNoViolations(
            "cross feature presentation imports must target a shared subpackage listed in SHARED_PRESENTATION",
            violations { source, import ->
                val target = import.getOrNull(1)
                source.feature != null &&
                    import.first() == "feature" &&
                    import.getOrNull(2) == "presentation" &&
                    target != source.feature &&
                    run {
                        val shared = SHARED_PRESENTATION[target].orEmpty()
                        val sub = import.getOrNull(3)
                        val inSharedPackage =
                            sub != null && sub in featureDirs(target!!, "presentation") && sub in shared
                        "*" !in shared && !inSharedPackage
                    }
            },
        )

    @Test
    fun `the shared parts of a feature do not depend on its screens`() =
        assertNoViolations(
            "a shared presentation subpackage must not import the root presentation package of its own feature",
            violations { source, import ->
                val shared = SHARED_PRESENTATION[source.feature].orEmpty()
                val sub = source.pkg.getOrNull(3)
                source.layer == "presentation" &&
                    sub != null &&
                    sub in shared &&
                    import.first() == "feature" &&
                    import.getOrNull(1) == source.feature &&
                    import.getOrNull(2) == "presentation" &&
                    // a file straight in presentation, not in one of its subpackages
                    import.getOrNull(3)?.let { it !in featureDirs(source.feature!!, "presentation") } == true
            },
        )
}
