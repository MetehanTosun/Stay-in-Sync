declare module 'swagger-client' {
  interface SwaggerClientOptions {
    url?: string;
    spec?: any;
    [key: string]: any;
  }

  interface SwaggerClientResult {
    spec: any;
    [key: string]: any;
  }

  interface SwaggerClient {
    resolve(options: SwaggerClientOptions): Promise<SwaggerClientResult>;
    [key: string]: any;
  }

  const SwaggerClient: SwaggerClient;
  export = SwaggerClient;
}