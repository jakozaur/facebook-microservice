# Facebook service written for ScalaWaw #4 Hackhathon

More details on [Meetup page](http://www.meetup.com/ScalaWAW/events/228083916/).

[![Join the chat at https://gitter.im/theiterators/akka-http-microservice](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/theiterators/akka-http-microservice?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Deploy to Heroku](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

## Usage

Start services with sbt:

```
$ sbt
> ~re-start
```

With the service up, you can start sending HTTP requests:

```
$ curl http://localhost:9000/ip/8.8.8.8
{
  "city": "Mountain View",
  "ip": "8.8.8.8",
  "latitude": 37.386,
  "country": "United States",
  "longitude": -122.0838
}
```

```
$ curl -X POST -H 'Content-Type: application/json' http://localhost:9000/ip -d '{"ip1": "8.8.8.8", "ip2": "8.8.4.4"}'
{
  "distance": 2201.448386715217,
  "ip1Info": {
    "city": "Mountain View",
    "ip": "8.8.8.8",
    "latitude": 37.386,
    "country": "United States",
    "longitude": -122.0838
  },
  "ip2Info": {
    "ip": "8.8.4.4",
    "country": "United States",
    "latitude": 38.0,
    "longitude": -97.0
  }
}
```

### Testing

Execute tests using `test` command:

```
$ sbt
> test
```

## Author & license

 * [Martyna Gula](https://github.com/biesczadka)
 * [Jacek Migdał](https://github.com/jakozaur)

Based on [akka-http-microservice](https://github.com/theiterators/akka-http-microservice) by:

Łukasz Sowa <lukasz@theiterators.com> from [Iterators](http://www.theiterators.com).

For licensing info see LICENSE file in project's root directory.
