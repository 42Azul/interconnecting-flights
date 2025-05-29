# Ryanair Task - Interconnecting Flights API

A Spring Boot RESTful API that provides information about direct and interconnected Ryanair flights (with at most one stop) between two airports, within a specified date-time range.

## Features

- Retrieves data from Ryanair public APIs:
  - **Routes API**: Retrieves valid direct routes (RYANAIR-operated only, no connections).
  - **Schedules API**: Retrieves flight schedules by month.
- Supports:
  - Direct flights
  - Interconnected flights with one stop and minimum 2h delay.
- Filters results based on provided departure/arrival datetimes.