## Running

Run this using [sbt](http://www.scala-sbt.org/).:

```
sbt run
```

And then go to http://localhost:9000 to see the running web application.

## Routes

GET /hotel >>>>>>>>>>>>>>>>>>>>>>> Get all hotels list

GET /hotel/:city >>>>>>>>>>>>>>>>> Search hotels by city

parameters:
- orderby (ASC/DESC)
- apikey (require some random text to request)

example: http://localhost:9000/hotel/Bangkok?orderby=DESC&apikey=testservice
