package com.brightminds.rebuild.common.exception;

import java.util.List;

public record ValidationErrorData(List<FieldViolation> violations) {
}
