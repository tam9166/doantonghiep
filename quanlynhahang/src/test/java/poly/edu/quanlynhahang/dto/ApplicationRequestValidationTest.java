package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ApplicationRequestValidationTest {
    private static Validator validator;

    @BeforeAll
    static void initializeValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validMultipartFieldsPassBeforeFileContentValidation() {
        ApplicationUploadRequest request = new ApplicationUploadRequest();
        request.setFullname("Nguyễn Văn An");
        request.setPhone("0901234567");
        request.setEmail("an@example.test");
        request.setMessage("Ứng tuyển vị trí phục vụ");
        request.setPostId(12);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void malformedIdentityAndMissingRecruitmentPostAreRejected() {
        ApplicationUploadRequest request = new ApplicationUploadRequest();
        request.setFullname(" ");
        request.setPhone("not-a-phone");
        request.setEmail("not-an-email");

        var fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();

        assertTrue(fields.contains("fullname"));
        assertTrue(fields.contains("phone"));
        assertTrue(fields.contains("email"));
        assertTrue(fields.contains("postId"));
        assertFalse(fields.isEmpty());
    }
}
