package com.daesungiot.gateway.binary;

import com.daesungiot.gateway.daesung.RemoteHandler;
import com.daesungiot.gateway.daesung.RemoteMessageDecoder;
import com.daesungiot.gateway.daesung.RemoteMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Getter
@Setter
@NoArgsConstructor
@Configuration
public class BinaryServer implements ApplicationContextAware {

    @Value("${server.gatewayPort}")
    private int port;
    private boolean ssl;
    private String decoder = "decoder";
    private String encoder = "encoder";
    private String handler = "handler";
    private SslContext sslCtx;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;


    private static ApplicationContext context;

    @PostConstruct
    public void init(){
        System.out.println("BinaryServer -> init Called");
        startup();
    }

    public void startup() {
        System.out.println("BinaryServer -> startup CALLED");
        System.out.println("ssl :" + ssl);
        if (ssl) {
            SelfSignedCertificate ssc;
            try {
                ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (Exception e) {
                System.out.println(e);
            }
        } else {
            sslCtx = null;
        }

        bossGroup = new NioEventLoopGroup(); // (1)
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    // ChannelInitializer 클래스를 통해 서버에 필요한 ChannelHandler들을 모두 등록한다.
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)

                        @Override
                        public void initChannel(SocketChannel ch) throws RuntimeException {
                            System.out.println("BinaryServer -> startup -> initChannel CALLED");
                            try {
                                BinaryMessageEncoder bmEncoder = (BinaryMessageEncoder)context.getBean(encoder);
                                BinaryMessageDecoder bmDecoder = new RemoteMessageDecoder();
                                // @Sharable handler 오류 해결 2024-04-17  참조 블로그 : https://velog.io/@joosing/take-a-look-netty
//                                BinaryMessageDecoder bmDecoder = (BinaryMessageDecoder)context.getBean(decoder);
                                BinaryHandler bHandler = (BinaryHandler)context.getBean(handler);

                                ChannelPipeline p = ch.pipeline();
                                if (sslCtx != null) {
                                    p.addLast(sslCtx.newHandler(ch.alloc()));
                                }

                                System.out.println("p.addLast(bmEncoder, bmDecoder, bHandler); ");
                                p.addLast(bmEncoder, bmDecoder, bHandler); // 에러 라인

                            } catch (Exception e) {
                                System.out.println(e);
                            }
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.TCP_NODELAY, true)   // 응답을 바로 주기 위해 추가..
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            b.bind(port).sync(); // (7)
            System.out.println("initChannel END");
            System.out.println("The binaryServer startup is completed (port:"+port+")");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void shutdown() {
        System.out.println("BinaryServer -> shutdown CALLED");

        try {
            workerGroup.shutdownGracefully().sync();
            bossGroup.shutdownGracefully().sync();
            System.out.println("The binaryServer shutdown is completed (port:"+port+")");
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }



    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        System.out.println("BinaryServer -> setApplicationContext CALLED");
        context = ctx;
    }

    public static ApplicationContext getContext() {
        System.out.println("BinaryServer -> ApplicationContext CALLED");
        return context;
    }
}
