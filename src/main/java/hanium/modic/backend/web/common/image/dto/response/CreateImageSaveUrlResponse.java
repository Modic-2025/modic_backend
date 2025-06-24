package hanium.modic.backend.web.common.image.dto.response;

public record CreateImageSaveUrlResponse(
	String imageSaveUrl,
	String imagePath
) {
}
