package ru.ifmo.se.pisk.web.form;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class AvatarUploadForm {

    @NotNull
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
