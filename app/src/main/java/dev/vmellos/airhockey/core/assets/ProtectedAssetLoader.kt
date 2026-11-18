package dev.vmellos.airhockey.core.assets

import android.content.Context
import dev.vmellos.airhockey.assetcrypto.AssetVaultCipher
import dev.vmellos.airhockey.assetcrypto.AssetVaultFormat
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Arrays

/**
 * Carregador protegido de ativos em memória (RAM).
 *
 * Lê blobs opacos `lspk/<hash>.lpkg`, autentica e descriptografa via AES-256-GCM
 * (vinculado ao caminho canônico via subchave e AAD) e zera todos os buffers temporários
 * imediatamente após o upload para GPU/parser, sem gravar arquivos em disco ou cache.
 */
class ProtectedAssetLoader(
    private val openObfuscatedAsset: (String) -> InputStream,
    private val masterKeyProvider: () -> ByteArray = { GeneratedAssetVaultKey.reconstructMasterKey() }
) {
    constructor(context: Context) : this(
        openObfuscatedAsset = { obfuscatedPath -> context.assets.open(obfuscatedPath) }
    )

    /**
     * Autentica e descriptografa em RAM o ativo de [canonicalPath] e entrega o [ByteBuffer]
     * para [consumer], zerando a chave mestra e o buffer descriptografado em seguida.
     */
    fun <T> withDecryptedBuffer(
        canonicalPath: String,
        consumer: (ByteBuffer) -> T
    ): T {
        val obfuscatedPath = AssetVaultFormat.obfuscatePath(canonicalPath)
        val masterKey = masterKeyProvider()
        var plaintext: ByteArray? = null
        try {
            plaintext = openObfuscatedAsset(obfuscatedPath).use { stream ->
                AssetVaultCipher.decrypt(masterKey, canonicalPath, stream)
            }
            return consumer(ByteBuffer.wrap(plaintext))
        } finally {
            Arrays.fill(masterKey, 0.toByte())
            if (plaintext != null) {
                Arrays.fill(plaintext, 0.toByte())
            }
        }
    }

    /**
     * Carrega uma [ModelInstance] do Sceneview/Filament diretamente a partir do [ByteBuffer]
     * descriptografado em RAM, limpando o buffer temporário logo após o upload.
     */
    fun loadModelInstance(
        modelLoader: ModelLoader,
        canonicalPath: String
    ): ModelInstance = withDecryptedBuffer(canonicalPath) { buffer ->
        modelLoader.createModelInstance(buffer)
    }
}
