build:
	source ./bin/check_java.sh && ./bin/sbt compile test

publish: build
	source ./bin/check_java.sh && ./bin/sbt docker:publishLocal

create:
	docker network create app-tier
	docker-compose up --build -d

test:
	-docker rm mailiranitar-test
	docker build --pull ./locust -t mailiranitar-test
	docker run -e TARGET_URL=http://mailiranitar:8888 --network app-tier --name mailiranitar-test mailiranitar-test:latest -f /mnt/locustfile.py --headless --host http://mailiranitar:8888 --users=200 --hatch-rate 10 --run-time 180

clean:
	-docker-compose down
	-docker-compose rm
	-docker rm mailiranitar-test
	-docker network rm app-tier

locallocust:
	locust -f locust/locustfile.py --headless -u 3000 -r 100 -H http://localhost:8888

