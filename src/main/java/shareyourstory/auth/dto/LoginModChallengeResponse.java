package shareyourstory.auth.dto;

/**
 * Respuesta del paso 1 del login de moderador: el password ya se ha validado y
 * se entrega un challengeId efimero que el paso 2 (TOTP) debe presentar. Ata los
 * dos factores: sin este challengeId no se puede obtener el token.
 */
public record LoginModChallengeResponse(String challengeId, boolean requires2fa) {
}
