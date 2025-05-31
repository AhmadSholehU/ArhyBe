package com.overdevx.arhybe.di

import com.overdevx.arhybe.model.PredictionResult
import com.overdevx.arhybe.network.EcgService
import com.overdevx.arhybe.network.EcgWebSocketListener
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.Channel
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://192.168.7.85:8000/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())  // Tambahkan factory untuk kelas Kotlin
            .build()
    }
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(moshi: Moshi, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideEcgService(retrofit: Retrofit): EcgService {
        return retrofit.create(EcgService::class.java)
    }

    /**
     * Channel untuk mentransmisikan data ECG yang diterima lewat WebSocket
     * Misalnya Channel<List<Float>> untuk setiap batch ECG.
     */
    @Provides
    @Singleton
    fun provideEcgChannel(): Channel<List<Float>> = Channel(Channel.BUFFERED)

    /**
     * Channel untuk mentransmisikan hasil prediksi (jika server mengirim lewat WebSocket).
     * Tipe payload bisa berupa data class PredictionResult (akan kita definisikan nanti).
     */
    @Provides
    @Singleton
    fun providePredictionChannel(): Channel<PredictionResult> = Channel(Channel.BUFFERED)

    @Provides
    @Singleton
    fun provideWebSocketListener(
        ecgChannel: Channel<List<Float>>,
        predictionChannel: Channel<PredictionResult>,
        moshi: Moshi
    ): EcgWebSocketListener {
        return EcgWebSocketListener(ecgChannel, predictionChannel, moshi)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(
        okHttpClient: OkHttpClient,
        listener: EcgWebSocketListener
    ): WebSocket {
        val request = Request.Builder()
            .url("ws://192.168.7.85:8000/ws")
            .build()
        return okHttpClient.newWebSocket(request, listener)
    }
}