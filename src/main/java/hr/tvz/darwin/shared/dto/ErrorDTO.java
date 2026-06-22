package hr.tvz.darwin.shared.dto;

import java.io.Serial;
import java.io.Serializable;

/** Server → client: a protocol-safe error message without a remote stack trace. */
public record ErrorDTO(String errorMessage) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
