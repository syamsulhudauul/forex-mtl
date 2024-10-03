# forex-mtl

Live interpeter from one time service. 
the application will provide you this logic 

/* */
The service returns an exchange rate when provided with 2 supported currencies
The rate should not be older than 5 minutes
The service should support at least 10,000 successful requests per day with 1 API token

one-service API can just only serve 1000 call per day. 

#How to run this local project 
- run one frame service with 'docker-compose up -d'
- Compile n run the Project
    -  enter sbt console
    -  execute compile
    -  run
- Run Testing 
    - test -> via sbt console. 

