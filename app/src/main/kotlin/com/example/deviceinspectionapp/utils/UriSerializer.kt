import android.net.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializer(forClass = Uri::class)
object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Uri")

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())  // Сохраняем URI как строку
    }

    override fun deserialize(decoder: Decoder): Uri {
        val string = decoder.decodeString()  // Восстанавливаем строку в URI
        return Uri.parse(string)
    }
}
