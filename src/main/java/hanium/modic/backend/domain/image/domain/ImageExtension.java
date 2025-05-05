package hanium.modic.backend.domain.image.domain;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import lombok.Getter;

@Getter
public enum ImageExtension {
	JPG("jpg"),
	JPEG("jpeg"),
	PNG("png"),
	GIF("gif"),
	SVG("svg"),
	BMP("bmp"),
	TIFF("tiff"),
	WEBP("webp"),
	HEIC("heic"),
	ICO("ico"),
	;

	private final String extension;

	ImageExtension(String extension) {
		this.extension = extension;
	}

	// 확장자 유효성 검사
	public static boolean isValidExtension(String extension) {
		for (ImageExtension imageExtension : ImageExtension.values()) {
			if (imageExtension.getExtension().equalsIgnoreCase(extension)) {
				return true;
			}
		}
		return false;
	}

	public static ImageExtension from(String extension) {
		for (ImageExtension imageExtension : ImageExtension.values()) {
			if (imageExtension.getExtension().equalsIgnoreCase(extension)) {
				return imageExtension;
			}
		}
		throw new AppException(ErrorCode.INVALID_IMAGE_FILE_NAME_EXCEPTION);
	}
}
