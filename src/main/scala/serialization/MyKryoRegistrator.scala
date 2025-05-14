package serialization

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}
import org.apache.spark.serializer.KryoRegistrator
import java.nio.ByteBuffer
import org.apache.spark.sql.types._

/**
 * Serializador Kryo personalizado para ByteBuffer.
 */
class ByteBufferSerializer extends Serializer[ByteBuffer] {
  /**
   * Serializa un ByteBuffer escribiendo su tamaño y contenido.
   */
  override def write(kryo: Kryo, output: Output, buffer: ByteBuffer): Unit = {
    val pos   = buffer.position()
    val limit = buffer.limit()
    val size  = limit - pos
    val bytes = new Array[Byte](size)
    buffer.get(bytes)
    buffer.position(pos)
    output.writeInt(size)
    output.writeBytes(bytes)
  }

  /**
   * Deserializa un ByteBuffer leyendo tamaño y contenido.
   */
  override def read(kryo: Kryo, input: Input, t: Class[ByteBuffer]): ByteBuffer = {
    val size  = input.readInt()
    val bytes = input.readBytes(size)
    ByteBuffer.wrap(bytes)
  }
}

/**
 * Registrador personalizado de clases para Kryo en Spark.
 */
class MyKryoRegistrator extends KryoRegistrator {
  /**
   * Registra las clases y serializadores necesarios en Kryo.
   *
   * @param kryo Instancia de Kryo a configurar.
   */
  override def registerClasses(kryo: Kryo): Unit = {
    kryo.register(classOf[org.apache.spark.sql.Row])
    kryo.register(classOf[Array[org.apache.spark.sql.Row]])
    kryo.register(Class.forName("org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema"))
    kryo.register(Class.forName("org.apache.spark.sql.catalyst.expressions.GenericRow"))
    kryo.register(Class.forName("[Lorg.apache.spark.sql.catalyst.expressions.GenericRow;"))

    kryo.register(classOf[ByteBuffer], new ByteBufferSerializer())

    // Registro de tipos Spark SQL comunes
    kryo.register(TimestampType.getClass)
    kryo.register(StringType.getClass)
    kryo.register(IntegerType.getClass)
    kryo.register(StructType.getClass)
    kryo.register(StructField.getClass)
    kryo.register(ArrayType.getClass)
    kryo.register(DecimalType.getClass)
    kryo.register(classOf[Array[DataType]])
    kryo.register(NullType.getClass)
  }
}
