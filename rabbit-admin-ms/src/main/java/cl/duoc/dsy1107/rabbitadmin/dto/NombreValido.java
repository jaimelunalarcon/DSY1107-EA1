package cl.duoc.dsy1107.rabbitadmin.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Nombre de cola o exchange en una URL: mismas reglas que los DTO. */
@NotBlank
@Size(max = NombresRabbit.LARGO_MAXIMO)
@Pattern(regexp = NombresRabbit.PATRON)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NombreValido {

    String message() default NombresRabbit.MENSAJE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
