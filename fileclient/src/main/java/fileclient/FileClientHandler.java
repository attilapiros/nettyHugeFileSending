/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package fileclient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Handles a client-side channel.
 */
public class FileClientHandler extends ChannelInboundHandlerAdapter {

  private FileOutputStream outputStream;

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    File file = new File("client.big");
    if (file.exists()) {
      file.delete();
    }
    file.createNewFile();
    outputStream = new FileOutputStream(file);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    System.out.println(UnpooledByteBufAllocator.DEFAULT.metric());
    System.out.println(PooledByteBufAllocator.DEFAULT.metric());

    // random computation
    Thread.sleep(1000);

    final ByteBuf buffer = ((ByteBuf) msg);
    ByteBuffer byteBuffer = buffer.nioBuffer();
    try {
      FileChannel localFileChannel = outputStream.getChannel();
      localFileChannel.write(byteBuffer);
      localFileChannel.force(false);
    } finally {
      buffer.release();
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    outputStream.close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
