# Zstreamer

一个使用netty编写的支持rtmp推流，http-flv拉流的小工具 

目前支持: OBS推流、Http(s)-Flv拉流、Http(s)使用Binary直接上传文件、Http(s)下载文件 

可以在commons的Config类中配置自己想要配置的内容

## 1.使用

在ide中直接运行或者打成jar包运行

### 1.推流

1. 在Obs的url中填写`rtmp://localhost:RTMP_PORT/your/url`
2. 在Obs的串流密钥中填写`房间名称`
3. 按照自己的喜好进行推流

### 2.拉流

1. 拉流地址为`http(s)://localhost:HTTP_PORT/live/audience/房间名称` 
2. 可以使用`bilibili`提供的flv.js进行拉流测试，网址为[flv.js demo (v1.4.0) (bilibili.github.io)](http://bilibili.github.io/flv.js/demo/) 
3. 也可以使用`VLC`进行拉流测试，在网络串流中填写拉流地址即可

### 3.上传/下载文件

1. Http服务只提供Binary上传/下载，无法使用formdata进行文件传输（匹配字符串很烦，而且速度太慢*为什么要用boundary分割令我很迷惑，写一个length字段既简洁又高效*）

2. 上传/下载文件的url：`http(s)://localhost:1937/file/fileName` post是上传，get是下载

3. 这里提供一个前端直接传文件流的模板(Vue+Axios)

   ```html
   <template>
       <div>
           <input type="file" @change="onchangeFile" />
           <button @click="onsubmitFile()"></button>
       </div>
   </template>
   
   <script>
       import axios from 'axios';
       export default {
           methods: {
               onchangeFile: function(event) {
                   this.file = event.target.files[0]
               },
               onsubmitFile: function() {
                   axios({
                       url: 'http://localhost:1937/file/fileName',
                       method: 'post',
                       data: this.file
                   }).
                   then((delegate) => {
                       console.log(delegate)
                   }).
                   catch((error) => {
                       console.log(error)
                   })
               }
           },
           data() {
               return {
                   file: null
               }
           }
       }
   </script>
   ```

## 2.配置

### 1.总体配置

项目里没有多少需要配置的地方，所以目前使用Java类进行配置

```java
package zstreamer.commons;

public class Config {
    /**
     * 下面两个参数分别为需要自动注入的handler的包名和文件上传/下载的根路径
     */
    public static final String HANDLER_PACKAGE = "zstreamer.http";
    public static final String BASE_URL = "C:\\Users\\31405\\Desktop\\";
    /**
     * 下面三个参数分别为：占位符起始符、占位符终止符、占位符统一替换成的字符
     */
    public static final String PLACE_HOLDER_START = "{";
    public static final String PLACE_HOLDER_END = "}";
    public static final String PLACE_HOLDER_REPLACER = "*";

    /**
     * 下面三个参数分别为：http的端口、rtmp的端口、是否启动https
     */
    public static final int HTTP_PORT = 1937;
    public static final int RTMP_PORT = 1936;
    public static final boolean SSL_ENABLED = false;
}
```

### 3.Http路径配置

1. 所有的Http服务的handler需要继承`AbstractHttpHandler`，同时放在自定义的basePackage下

2. 可以使用`@RequestPath`对handler对应的路径进行配置

   ```
   形如：/a/{b}/c
   {paramName}可以将对应位置的值解析成 paramName=value的键值对
   
   比如/file/{fileName}的RequestPath
   传入 /file/picture?time=2022
   解析成 fileName=picture、time=2022
   
   请注意占位符{}的优先级是最低的
   /a/{b}/c和/a/file/c
   /a/file/c?time=2022会进入/a/file/c
   ```

## 3.Http服务

Handler注册：`BasePackageClassloader`、`HandlerClassResolver`

请求分发：`RequestDispatcher`

Http业务处理：`AbstractHttpHandler`、`WrappedHttpRequest`

未完待续
