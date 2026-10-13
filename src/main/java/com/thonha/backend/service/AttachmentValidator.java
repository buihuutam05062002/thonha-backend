package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


@Component
public class AttachmentValidator {

    public static final int MAX_IMAGES = 5;
    public static final int MAX_VIDEOS = 1;
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    public static final long MAX_VIDEO_BYTES = 30L * 1024 * 1024;

    public enum Kind { IMAGE, VIDEO }

    public record Validated(MultipartFile file, Kind kind) {
    }

    public List<Validated> validate(List<MultipartFile> files) {
        List<Validated> result = new ArrayList<>();
        if (files == null) {
            return result;
        }
        int images = 0;
        int videos = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            Kind kind = detect(file);
            if (kind == null) {
                throw new ApiException(ErrorCode.INVALID_FILE_TYPE,
                        "Chỉ nhận ảnh JPG, PNG, WEBP hoặc video MP4");
            }
            if (kind == Kind.VIDEO) {
                if (++videos > MAX_VIDEOS) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST,
                            "Chỉ được đính kèm tối đa " + MAX_VIDEOS + " video");
                }
                if (file.getSize() > MAX_VIDEO_BYTES) {
                    throw new ApiException(ErrorCode.FILE_TOO_LARGE, "Video tối đa 30 MB");
                }
            } else {
                if (++images > MAX_IMAGES) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST,
                            "Chỉ được đính kèm tối đa " + MAX_IMAGES + " ảnh");
                }
                if (file.getSize() > MAX_IMAGE_BYTES) {
                    throw new ApiException(ErrorCode.FILE_TOO_LARGE, "Mỗi ảnh tối đa 5 MB");
                }
            }
            result.add(new Validated(file, kind));
        }
        return result;
    }


    public void requireImage(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty() || detect(file) != Kind.IMAGE) {
            throw new ApiException(ErrorCode.INVALID_FILE_TYPE, "Chỉ nhận ảnh JPG, PNG hoặc WEBP");
        }
        if (file.getSize() > maxBytes) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE,
                    "Ảnh tối đa " + (maxBytes / (1024 * 1024)) + " MB");
        }
    }

    private Kind detect(MultipartFile file) {
        byte[] h = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(h, 0, h.length);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Không đọc được tệp đính kèm");
        }
        if (read < 12) {
            return null;
        }
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return Kind.IMAGE;
        }
        if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return Kind.IMAGE;
        }
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return Kind.IMAGE;
        }
        if (h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p') {
            return Kind.VIDEO;
        }
        return null;
    }
}