package shareyourstory.auth.dto;

/**
 * Paso 2 del login de moderador: el challengeId emitido en el paso 1 (password
 * verificado) mas el codigo TOTP. El code viaja como String para soportar
 * ceros a la izquierda; se parsea a int en el controlador.
 */
public record VerifyLoginRequest(String challengeId, String code) {
}
