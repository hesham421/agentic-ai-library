package io.agenticai.reg.port;

import java.util.List;
import java.util.Objects;

/**
 * One package folder of the package directory, as read by the {@link PackageSource}
 * (REQ-REG-005, REQ-REG-062, REQ-REG-069, REQ-REG-071). A plain value: the port reads, it
 * decides nothing — every rule on this content (RULE-REG-001, RULE-REG-018, RULE-REG-020,
 * RULE-REG-024, RULE-REG-026, …) is the load run's.
 *
 * @param folderName     the folder's name — the {@code folder} of the load report
 * @param knowledgeText  the text of the service knowledge file ({@link PackageSource#KNOWLEDGE_FILE});
 *                       {@code null} when the file is absent or could not be read
 * @param definitionText the text of the service definition file ({@link PackageSource#DEFINITION_FILE});
 *                       {@code null} when the file is absent or could not be read
 * @param otherFileNames the name of every other entry of the folder, files or directories,
 *                       in name order (RULE-REG-020 is judged on them)
 * @param stable         whether the size and last-modified time of every file read were equal
 *                       before and after its read (RULE-REG-024)
 * @param unreadableFile {@code null}, or the name of the first entry whose read failed with an
 *                       {@code IOException} (RULE-REG-026); the folder's own name when the
 *                       folder itself could not be listed
 */
public record PackageFolder(
        String folderName,
        String knowledgeText,
        String definitionText,
        List<String> otherFileNames,
        boolean stable,
        String unreadableFile) {

    public PackageFolder {
        Objects.requireNonNull(folderName, "folderName");
        otherFileNames = otherFileNames == null ? List.of() : List.copyOf(otherFileNames);
    }
}
