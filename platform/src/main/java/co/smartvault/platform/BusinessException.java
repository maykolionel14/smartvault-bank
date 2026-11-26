package co.smartvault.platform;

public class BusinessException extends RuntimeException {
    private final int httpStatus;
    public BusinessException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
    public int httpStatus() {
        return httpStatus;
    }
    public static BusinessException invalid(String message) {
        return new BusinessException(422, message);
    }
    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }
    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }
}
