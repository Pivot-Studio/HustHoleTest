package cn.pivotstudio.husthole.moduleb.network;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import cn.pivotstudio.husthole.moduleb.network.errorhandler.ExceptionHandler;
import cn.pivotstudio.husthole.moduleb.network.errorhandler.HttpErrorHandler;
import cn.pivotstudio.husthole.moduleb.network.interceptor.RequestInterceptor;
import cn.pivotstudio.husthole.moduleb.network.interceptor.ResponseInterceptor;
import cn.pivotstudio.husthole.moduleb.network.interceptor.TokenInterceptor;
import cn.pivotstudio.husthole.moduleb.network.interceptor.TokenInterceptorV2;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @classname: NetworkApi
 * @description:
 * @date: 2022/4/29 14:31
 * @version: 1.0
 * @author:
 */
public class NetworkApi {

    /**
     * 获取APP运行状态及版本信息，用于日志打印
     */
    private static INetworkRequiredInfo iNetworkRequiredInfo;

    /**
     * API访问地址
     */
    private static String BASE_URL = null;

    private static OkHttpClient okHttpClient;
    private static OkHttpClient okHttpClientV2;

    private static final HashMap<String, Retrofit> retrofitHashMap = new HashMap<>();

    /**
     * 初始化
     */
    public static void init(INetworkRequiredInfo networkRequiredInfo) {
        iNetworkRequiredInfo = networkRequiredInfo;
    }

    /**
     * 创建serviceClass的实例
     */
    public static <T> T createService(Class<T> serviceClass, int type) {
        //设置Url类型
        setUrlType(type);
        return getRetrofit(serviceClass).create(serviceClass);
    }

    public static <T> T createService(Class<T> serviceClass) {
        //设置Url类型
        setUrlType(2);
        return getRetrofit(serviceClass).create(serviceClass);
    }

    /**
     * 设置访问Url类型
     *
     * @param type 0
     */
    private static void setUrlType(int type) {
        switch (type) {
            case 1:
                //测试版，已经不能用了
                BASE_URL = "https://hustholetest.pivotstudio.cn/api/";
                break;
            case 2:
                //正式版
                BASE_URL = "https://husthole.pivotstudio.cn/api/";
            default:
                break;
        }
    }

    /**
     * 配置OkHttp
     *
     * @return OkHttpClient
     */
    public static OkHttpClient getOkHttpClient() {
        //不为空则说明已经配置过了，直接返回即可。
        if (okHttpClient == null) {
            //OkHttp构建器
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            //设置缓存大小
            int cacheSize = 100 * 1024 * 1024;
            //设置网络请求超时时长，这里设置为6s
            builder.connectTimeout(6, TimeUnit.SECONDS);
            //添加请求拦截器，如果接口有请求头的话，可以放在这个拦截器里面
            builder.addInterceptor(new RequestInterceptor(iNetworkRequiredInfo));
            //添加返回拦截器，可用于查看接口的请求耗时，对于网络优化有帮助
            builder.addInterceptor(new ResponseInterceptor());

            builder.addInterceptor(new TokenInterceptor(iNetworkRequiredInfo.getApplicationContext()));
            //当程序在debug过程中则打印数据日志，方便调试用。
            if (iNetworkRequiredInfo != null && iNetworkRequiredInfo.isDebug()) {
                //iNetworkRequiredInfo不为空且处于debug状态下则初始化日志拦截器
                HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
                //设置要打印日志的内容等级，BODY为主要内容，还有BASIC、HEADERS、NONE。
                httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                //将拦截器添加到OkHttp构建器中
                builder.addInterceptor(httpLoggingInterceptor);
            }
            //OkHttp配置完成
            okHttpClient = builder.build();
        }
        return okHttpClient;
    }

    public static OkHttpClient getOkHttpClientV2() {
        if (okHttpClientV2 == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(6, TimeUnit.SECONDS)
                    .addInterceptor(new RequestInterceptor(iNetworkRequiredInfo))
                    .addInterceptor(new ResponseInterceptor())
                    .addInterceptor(new TokenInterceptorV2(iNetworkRequiredInfo.getApplicationContext()));
            if (iNetworkRequiredInfo != null && iNetworkRequiredInfo.isDebug()) {
                HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
                httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(httpLoggingInterceptor);
            }
            okHttpClientV2 = builder.build();
        }
        return okHttpClientV2;
    }

    /**
     * 配置Retrofit
     *
     * @param serviceClass 服务类
     * @return Retrofit
     */
    private static Retrofit getRetrofit(Class serviceClass) {
        if (retrofitHashMap.get(BASE_URL + serviceClass.getName()) != null) {
            //刚才上面定义的Map中键是String，值是Retrofit，当键不为空时，必然有值，有值则直接返回。
            return retrofitHashMap.get(BASE_URL + serviceClass.getName());
        }
        //初始化Retrofit  Retrofit是对OKHttp的封装，通常是对网络请求做处理，也可以处理返回数据。
        //Retrofit构建器
        Retrofit.Builder builder = new Retrofit.Builder();

        //设置访问地址
        builder.baseUrl(BASE_URL);

        //设置OkHttp客户端，传入上面写好的方法即可获得配置后的OkHttp客户端。
        builder.client(getOkHttpClient());

        //设置数据解析器 会自动把请求返回的结果（json字符串）通过Gson转化工厂自动转化成与其结构相符的实体Bean
        builder.addConverterFactory(GsonConverterFactory.create());

        //设置请求回调，使用RxJava 对网络返回进行处理
        builder.addCallAdapterFactory(RxJava2CallAdapterFactory.create());

        //retrofit配置完成
        Retrofit retrofit = builder.build();

        //放入Map中
        retrofitHashMap.put(BASE_URL + serviceClass.getName(), retrofit);

        //最后返回即可
        return retrofit;
    }

    /**
     * 配置RxJava 完成线程的切换
     *
     * @param observer 这个observer要注意不要使用lifecycle中的Observer
     * @param <T>      泛型
     * @return Observable
     * @deprecated 不符合RxJava链式调用风格，需要替换成
     * {@link cn.pivotstudio.husthole.moduleb.network.NetworkApi#applySchedulers()}
     */
    @Deprecated
    public static <T> ObservableTransformer<T, T> applySchedulers(final Observer<T> observer) {
        return upstream -> {
            Observable<T> observable = upstream
                    .subscribeOn(Schedulers.io())//线程订阅
                    .observeOn(AndroidSchedulers.mainThread())//观察Android主线程
                    .map(NetworkApi.getAppErrorHandler())//判断有没有500的错误，有则进入getAppErrorHandler
                    .onErrorResumeNext(new HttpErrorHandler<>());//判断有没有400的错误
            //订阅观察者
            observable.subscribe(observer);
            return observable;
        };
    }

    public static <T> ObservableTransformer<T, T> applySchedulers() {
        return upstream -> upstream
                .subscribeOn(Schedulers.io())//线程订阅
                .observeOn(AndroidSchedulers.mainThread())//观察Android主线程
                .map(NetworkApi.getAppErrorHandler())//判断有没有500的错误，有则进入getAppErrorHandler
                .onErrorResumeNext(new HttpErrorHandler<>());
    }

    /**
     * 错误码处理
     */
    protected static <T> Function<T, T> getAppErrorHandler() {
        return response -> {
            //当response返回出现500之类的错误时
            if (response instanceof BaseResponse && ((BaseResponse) response).responseCode >= 500) {
                //通过这个异常处理，得到用户可以知道的原因
                ExceptionHandler.ServerException exception = new ExceptionHandler.ServerException();
                exception.code = ((BaseResponse) response).responseCode;
                exception.message = ((BaseResponse) response).responseError != null ? ((BaseResponse) response).responseError : "";
                throw exception;
            }
            return response;
        };
    }
}

