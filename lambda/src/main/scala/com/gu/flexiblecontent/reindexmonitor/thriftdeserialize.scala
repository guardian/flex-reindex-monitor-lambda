package com.gu.flexiblecontent.reindexmonitor

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import org.apache.thrift.protocol.TCompactProtocol
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import org.apache.thrift.transport.TIOStreamTransport
import com.gu.flexiblecontent.model.thrift.Event
import scala.util.Try

// import com.twitter.scrooge.{ ThriftStruct, ThriftStructCodec }
// import org.apache.thrift.protocol.TCompactProtocol
// import org.apache.thrift.transport.TIOStreamTransport
// import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream

// import java.nio.ByteBuffer
// import scala.util.Try

object ThriftDeserialiser {
  /**
   * Deserialize a Flexible API event from a Thrift-serialized byte buffer
   */
  def deserialiseEvent(buffer: ByteBuffer): Try[Event] = {
    Try {
      val settings = buffer.get()
      val compressionType = compression(settings)
      compressionType match {
        case NoneType => payload(buffer)
        case GzipType => payload(GzipCompression.uncompress(buffer))
        case ZstdType => payload(ZstdCompression.uncompress(buffer))
      }
    }
  }

  private def compression(settings: Byte): CompressionType = {
    val compressionMask = 0x07.toByte
    val compressionType = (settings & compressionMask).toByte
    compressionType match {
      case 0 => NoneType
      case 1 => GzipType
      case 2 => ZstdType
      case x => throw new RuntimeException(s"The compression type: $x is not supported")
    }
  }

  private def payload(buffer: ByteBuffer): Event = {
    val bbis = new ByteBufferBackedInputStream(buffer)
    val transport = new TIOStreamTransport(bbis)
    val protocol = new TCompactProtocol(transport)
    Event.decode(protocol)
  }

}
