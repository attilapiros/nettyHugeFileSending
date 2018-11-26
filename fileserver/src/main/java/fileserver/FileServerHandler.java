/*
 * Copyright 2014 The Netty Project
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

package fileserver;

import io.netty.channel.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;

import java.io.RandomAccessFile;

public class FileServerHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    sendHugeFile(ctx);
  }

  public void sendHugeFile(ChannelHandlerContext ctx) throws Exception {
    final String hugeFile = "big.zip";
    RandomAccessFile raf = null;
    long length = -1;
    try {
      raf = new RandomAccessFile(hugeFile, "r");
      length = raf.length();
    } catch (Exception e) {
      ctx.writeAndFlush("ERR: " + e.getClass().getSimpleName() + ": " + e.getMessage() + '\n');
      e.printStackTrace();
      return;
    } finally {
      if (length < 0 && raf != null) {
        raf.close();
      }
    }
    ChannelFuture future;

    if (ctx.pipeline().get(SslHandler.class) == null) {
      // SSL not enabled - can use zero-copy file transfer.
      future = ctx.writeAndFlush(new DefaultFileRegion(raf.getChannel(), 0, length));
    } else {
      // SSL enabled - cannot use zero-copy file transfer.
      future = ctx.writeAndFlush(new ChunkedFile(raf, 1024));
    }

    future.addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();

    if (ctx.channel().isActive()) {
      ctx.writeAndFlush("ERR: " +
          cause.getClass().getSimpleName() + ": " +
          cause.getMessage() + '\n').addListener(ChannelFutureListener.CLOSE);
    }
  }
}

