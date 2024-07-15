package de.louieleedev.fileuploadanddownload.resource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.copy;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@RestController
@RequestMapping("/file")
public class FileResource {

    // Definiere, wo die Datei gespeichert werden soll
    public static final String DIRECTORY = System.getProperty("user.home") + "/Downloads/uploads/";

    /**
     * Methode zum Hochladen (Upload) einer Datei
     *
     * Zuerst bereinigt man den Dateinamen, um Sicherheitsrisiken wie Pfadmanipulation zu vermeiden und
     * bestimmt den Pfad, wo die Datei gespeichert wird, normalisiert diesen, um Inkonsistenzen in den Pfaden zu vermeiden.
     * Mit 'copy' kopiert man den Inhalt der Datei in den Zielordner, überschreibt dabei eventuell vorhandene Dateien mit demselben Namen.
     * Am Ende gibt man eine Antwort mit HTTP-Status 200 (OK) zurück und sendet die Liste der gespeicherten Dateinamen.
     */
    @PostMapping("/upload")
    public ResponseEntity<List<String>> uploadFiles(@RequestParam("files")List<MultipartFile> multipartFiles) throws IOException {
        List<String> filenames = new ArrayList<>();
        for(MultipartFile file : multipartFiles) {
            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            Path fileStorage = get(DIRECTORY, filename).toAbsolutePath().normalize();
            copy(file.getInputStream(), fileStorage, REPLACE_EXISTING);
            filenames.add(filename);
        }
        return ResponseEntity.ok().body(filenames);
    }

    /**
     * Methode zum Herunterladen (Download) einer Datei
     *
     * Mit 'filePath' bestimmt man den vollständigen Pfad der Datei und überprüft, ob die Datei im Dateisystem existiert.
     * Falls die Datei nicht gefunden wird, wird eine Exception geworfen.
     * Man erstellt eine Ressource, die auf die Datei verweist.
     * Danach erstellt man HTTP-Headers für die Antwort, setzt den Dateinamen und die Anweisung zur Dateianlage (sodass der Browser einen Download-Dialog anzeigt).
     * Mit 'ResponseEntity.ok()' sendet man die Datei mit dem entsprechenden MIME-Typ als Antwort zurück.
     * */
    @GetMapping("download/{filename}")
    public ResponseEntity<Resource> downloadFiles(@PathVariable("filename") String filename) throws IOException {
        Path filePath = get(DIRECTORY).toAbsolutePath().normalize().resolve(filename);
        if(!Files.exists(filePath)) {
            throw new FileNotFoundException(filename + " konnte nicht im Server gefunden werden");
        }
        Resource resource = new UrlResource(filePath.toUri());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("File-Name", filename);
        httpHeaders.add(CONTENT_DISPOSITION, "attachment;File-Name=" + resource.getFilename());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(Files.probeContentType(filePath)))
                .headers(httpHeaders).body(resource);
    }
}
