package com.clearnotes.controller;

import com.clearnotes.dto.recording.UploadResponse;
import com.clearnotes.service.AudioUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/recordings")
public class RecordingUploadController {
    private final AudioUploadService uploads;

    public RecordingUploadController(AudioUploadService uploads) { this.uploads = uploads; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@AuthenticationPrincipal Jwt jwt,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam(defaultValue = "false") boolean deleteAudioAfterProcessing) {
        return uploads.upload(Long.valueOf(jwt.getSubject()), file, deleteAudioAfterProcessing);
    }
}
