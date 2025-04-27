package serialization

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}
import org.apache.spark.serializer.KryoRegistrator
import java.nio.ByteBuffer

import org.apache.spark.sql.types._

// Serializador personalizado para ByteBuffer
class ByteBufferSerializer extends Serializer[ByteBuffer] {
  override def write(kryo: Kryo, output: Output, buffer: ByteBuffer): Unit = {
    val pos = buffer.position()
    val limit = buffer.limit()
    val size = limit - pos
    val bytes = new Array[Byte](size)
    buffer.get(bytes)
    buffer.position(pos)
    output.writeInt(size)
    output.writeBytes(bytes)
  }

  override def read(kryo: Kryo, input: Input, t: Class[ByteBuffer]): ByteBuffer = {
    val size = input.readInt()
    val bytes = input.readBytes(size)
    ByteBuffer.wrap(bytes)
  }
}

// Registrator personalizado para Kryo
class MyKryoRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo): Unit = {
    // Registros estándar
    kryo.register(classOf[org.apache.spark.sql.Row])
    kryo.register(classOf[Array[org.apache.spark.sql.Row]])

    kryo.register(Class.forName("org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema"))
    kryo.register(Class.forName("org.apache.spark.sql.catalyst.expressions.GenericRow"))
    kryo.register(Class.forName("[Lorg.apache.spark.sql.catalyst.expressions.GenericRow;"))

    // Registro de ByteBuffer
    kryo.register(classOf[ByteBuffer], new ByteBufferSerializer())

    // 🚨 Registro del tipo TimestampType que causaba el error
    kryo.register(TimestampType.getClass)

    // ✅ Opcionalmente, registra otros tipos si los usas frecuentemente
    kryo.register(StringType.getClass)
    kryo.register(IntegerType.getClass)
    kryo.register(StructType.getClass)
    kryo.register(StructField.getClass)
    kryo.register(ArrayType.getClass)
    kryo.register(DecimalType.getClass)
  }
}