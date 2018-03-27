package com.gu.flexiblecontent.reindexmonitor

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream, IOException }
import java.nio.ByteBuffer
import org.apache.commons.io.IOUtils

sealed trait CompressionType
case object NoneType extends CompressionType
case object GzipType extends CompressionType
case object ZstdType extends CompressionType

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipCompression {

  def compress(data: Array[Byte]): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val out = new GZIPOutputStream(bos)
    out.write(data)
    out.close()
    bos.toByteArray
  }

  def uncompress(buffer: ByteBuffer): ByteBuffer = {
    val bos = new ByteArrayOutputStream(8192)
    val in = new GZIPInputStream(new ByteBufferBackedInputStream(buffer))
    IOUtils.copy(in, bos)
    in.close()
    bos.close()
    ByteBuffer.wrap(bos.toByteArray())
  }

  def uncompress(data: Array[Byte]): Array[Byte] = {
    val buffer = ByteBuffer.wrap(data)
    val byteBuffer = uncompress(buffer)
    val result = new Array[Byte](byteBuffer.remaining)
    byteBuffer.get(result)
    result
  }
}

import com.github.luben.zstd.ZstdInputStream
import com.github.luben.zstd.ZstdOutputStream

object ZstdCompression {

  def compress(data: Array[Byte]): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val out = new ZstdOutputStream(bos)
    out.write(data)
    out.close()
    bos.toByteArray
  }

  def uncompress(buffer: ByteBuffer): ByteBuffer = {
    val bos = new ByteArrayOutputStream(8192)
    val in = new ZstdInputStream(new ByteBufferBackedInputStream(buffer))
    IOUtils.copy(in, bos)
    in.close()
    bos.close()
    ByteBuffer.wrap(bos.toByteArray())
  }

  def uncompress(data: Array[Byte]): Array[Byte] = {
    val buffer = ByteBuffer.wrap(data)
    val byteBuffer = uncompress(buffer)
    val result = new Array[Byte](byteBuffer.remaining)
    byteBuffer.get(result)
    result
  }

}
