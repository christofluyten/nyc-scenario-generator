package scenariogenerator;

import com.github.christofluyten.routingtable.RoutingTable;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.RoutingTableSupplier;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.pdptw.common.*;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.util.TimeWindow;
import data.area.Area;
import data.area.JfkArea;
import data.area.ManhattanArea;
import data.area.NycArea;
import data.object.Passenger;
import data.object.SimulationObject;
import data.object.Taxi;
import data.time.Date;
import fileMaker.IOHandler;
import fileMaker.PassengerHandler;
import fileMaker.TaxiHandler;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by christof on 23.11.16.
 */
public class ScenarioGenerator {
    //    private static final String taxiDataDirectory = "D:/Taxi_data/";    //path to director with the FOIL-directories
    private static final String TAXI_DATA_DIRECTORY = "/media/christof/Elements/Taxi_data/";
    private static final String TRAVEL_TIMES_DIRECTORY = "/media/christof/Elements/Traffic_estimates/"; //path to director with the travel_times
    private static final Date TAXI_DATA_START_TIME = new Date("2013-11-18 16:00:00");                   //format: "yyyy-mm-dd HH:mm:ss"
    private static final Date TAXI_DATA_END_TIME = new Date("2013-11-18 17:00:00");

//    private static final Date TAXI_START_TIME = new Date("2013-11-18 16:00:00");
//    private static final Date TAXI_END_TIME = new Date("2013-11-18 17:00:00");
    private static final double MAX_VEHICLE_SPEED_KMH = 120d;

//    private static final long pickupDuration = 30 * 1000L;
//    private static final long deliveryDuration = 30 * 1000L;

    private static final long PICKUP_DURATION = 0L;
    private static final long DELIVERY_DURATION = 0L;


    private static final String SCENARIO_NAME = "TimeWindow";
    private static final int CUT_LENGTH = 500;                                                  //maximum length in meters of a edge in the graph (or "link" in the "map")

    private static final long SCENARIO_DURATION = (1 * 60 * 60 * 1000L) + 1L;

    private static final long SCENARIO_DURATION_DEBUG = (1000 * 1000L) + 1L;

    private static final boolean TRAFFIC = true;


    private static final long TICK_SIZE = 250L;

    private boolean ridesharing;
    private boolean debug;

    final Builder builder;


    private IOHandler ioHandler;
    private String scenarioName;

    public ScenarioGenerator(Builder builder) {
        this.builder = builder;
        scenarioName = builder.scenarioName;
        IOHandler ioHandler = new IOHandler();
        ioHandler.setTaxiDataDirectory(builder.taxiDataDirectory);
        ioHandler.setScenarioStartTime(builder.taxiDataStartTime);
        ioHandler.setScenarioEndTime(builder.taxiDataEndTime);
//        ioHandler.setTaxiStartTime(TAXI_DATA_START_TIME);
//        ioHandler.setTaxiEndTime(TAXI_DATA_END_TIME);
        ioHandler.setAttribute(builder.scenarioName);
        ioHandler.setCutLength(builder.cutLength);
        if (builder.traffic) {
            ioHandler.setTravelTimesDirectory(builder.travelTimesDirectory);
            ioHandler.setWithTraffic();
        }
        this.ioHandler = ioHandler;
        setScenarioFileFullName();
        makeMap();
    }

    public static void main(String[] args) throws Exception {

        ScenarioGenerator sg =
                ScenarioGenerator.builder()
                        .setCutLength(CUT_LENGTH)
                        .setDeliveryDuration(DELIVERY_DURATION)
                        .setPickupDuration(PICKUP_DURATION)
                        .setMaxVehicleSpeedKmh(MAX_VEHICLE_SPEED_KMH)
                        .setRidesharing(false)
                        .setScenarioDuration(SCENARIO_DURATION_DEBUG)
                        .setScenarioName("test")
                        .setTaxiDataDirectory(TAXI_DATA_DIRECTORY)
                        .setTravelTimesDirectory(TRAVEL_TIMES_DIRECTORY)
                        .setTaxiDataStartTime(TAXI_DATA_START_TIME)
                        .setTaxiDataEndTime(TAXI_DATA_END_TIME)
                        .setTickSize(TICK_SIZE)
                        .setTraffic(TRAFFIC)
                        .build();
        Scenario s = sg.generateTaxiScenario(true);
        System.out.println("scenario made");
    }

    public IOHandler getIoHandler() {
        return ioHandler;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    private void setScenarioFileFullName() {
        getIoHandler().setScenarioFileFullName(getIoHandler().getScenarioFileName() + "_" + getIoHandler().getAttribute() + "_" + getIoHandler().getScenarioStartTime().getShortStringDateForPath() + "_"
                + getIoHandler().getScenarioEndTime().getShortStringDateForPath());
    }

    private void makeMap() {
        try {
            getIoHandler().makeMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Scenario generateTaxiScenario(boolean debug) throws Exception {
        this.debug = debug;
        Scenario.Builder builder = Scenario.builder();
        addGeneralProperties(builder);
        if (debug) {
            builder.addModel(
                    PDPGraphRoadModel.builderForGraphRm(
                            RoadModelBuilders
                                    .staticGraph(
                                            ListenableGraph.supplier(DotGraphIO.getMultiAttributeDataGraphSupplier(Paths.get(getIoHandler().getMapFilePath()))))
                                    .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
                                    .withDistanceUnit(SI.KILOMETER)
//                                    .withRoutingTable(true)
                    )
                            .withAllowVehicleDiversion(true))
                    .addEvent(TimeOutEvent.create(SCENARIO_DURATION_DEBUG))
                    .scenarioLength(SCENARIO_DURATION_DEBUG);
//                    .addEvent(TimeOutEvent.create(scenarioDuration))
//                    .scenarioLength(scenarioDuration);
            addPassengersDebug(builder);
//            addPassengers(builder);
        } else {
            builder.addModel(
                    PDPGraphRoadModel.builderForGraphRm(
                            RoadModelBuilders
                                    .staticGraph(
                                            ListenableGraph.supplier(DotGraphIO.getMultiAttributeDataGraphSupplier(Paths.get(getIoHandler().getMapFilePath()))))
                                    .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
                                    .withDistanceUnit(SI.KILOMETER)
                                    .withRoutingTable(true)
                    )
                            .withAllowVehicleDiversion(true))
                    .addEvent(TimeOutEvent.create(SCENARIO_DURATION))
                    .scenarioLength(SCENARIO_DURATION);
            addPassengers(builder);
        }
        addTaxis(builder);
//            addJFK(builder);
//            addManhattan(builder);
//            addNYC(builder);
        Scenario scenario = builder.build();
        getIoHandler().writeScenario(scenario);
        return scenario;
//        }
    }


    private void addGeneralProperties(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        builder

//                .addModel(PDPRoadModel.builder(RoadModelBuilders.staticGraph(DotGraphIO.getLengthDataGraphSupplier(Paths.get(getIoHandler().getMapFilePath())))
//                .withDistanceUnit(SI.METER)
//                .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)))
//                .addModel(PDPRoadModel.builder(RoadModelBuilders.staticGraph(ListenableGraph.supplier(
//                                        (Supplier<? extends Graph<MultiAttributeData>>) DotGraphIO.getLengthDataGraphSupplier(Paths.get(getIoHandler().getMapFilePath()))))
//                        .withDistanceUnit(SI.METER)
//                        .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)))

                .addModel(TimeModel.builder()
                        .withRealTime()
                        .withStartInClockMode(RealtimeClockController.ClockMode.REAL_TIME)
                        .withTickLength(TICK_SIZE)
                        .withTimeUnit(SI.MILLI(SI.SECOND)))
                .addModel(
                        DefaultPDPModel.builder()

                                .withTimeWindowPolicy(TimeWindowPolicy.TimeWindowPolicies.TARDY_ALLOWED))
                .setStopCondition(StatsStopConditions.timeOutEvent())
                .addEvent(AddDepotEvent.create(-1, new Point(-73.9778627, -40.7888872)))
        ;
    }

    private void addTaxis(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        if (!(getIoHandler().fileExists(ioHandler.getPositionedTaxisPath()))) {
            TaxiHandler tfm = new TaxiHandler(ioHandler);
            tfm.extractAndPositionTaxis();
        }
        List<SimulationObject> taxis = getIoHandler().readPositionedObjects(ioHandler.getPositionedTaxisPath());
        int totalCount = 0;
        int addedCount = 0;
        for (SimulationObject object : taxis) {
            if (totalCount % 20 == 0) {
                addedCount++;
                Taxi taxi = (Taxi) object;
//            builder.addEvent(AddVehicleEvent.create(taxi.getStartTime(TAXI_START_TIME), VehicleDTO.builder()
                builder.addEvent(AddVehicleEvent.create(-1, VehicleDTO.builder()
                        .speed(MAX_VEHICLE_SPEED_KMH)
                        .startPosition(taxi.getStartPoint())
                        .capacity(4)
                        .build()));
            }


            totalCount++;
            if (debug && addedCount >= 10) {
                break;
            }
        }
        System.out.println(addedCount + " taxi's added of the " + totalCount);
    }


    private void addPassengers(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        if (!(getIoHandler().fileExists(ioHandler.getPositionedPassengersPath()))) {
            PassengerHandler pfm = new PassengerHandler(ioHandler);
            pfm.extractAndPositionPassengers();
        }
        List<SimulationObject> passengers = getIoHandler().readPositionedObjects(ioHandler.getPositionedPassengersPath());
        int totalCount = 0;
        int addedCount = 0;
//TODO path meegeven
        RoutingTable routingTable = RoutingTableSupplier.getRoutingTable("path");
        for (SimulationObject object : passengers) {
            if (true && (totalCount % 20 == 0)) {
                addedCount++;
                Passenger passenger = (Passenger) object;
                long pickupStartTime = passenger.getStartTime(TAXI_DATA_START_TIME);
                long pickupTimeWindow = passenger.getStartTimeWindow(TAXI_DATA_START_TIME);
                long deliveryStartTime = getDeliveryStartTime(passenger, routingTable);
                Parcel.Builder parcelBuilder = Parcel.builder(passenger.getStartPoint(), passenger.getEndPoint())
                        .orderAnnounceTime(pickupStartTime)
                        .pickupTimeWindow(TimeWindow.create(pickupStartTime, pickupStartTime + pickupTimeWindow))
                        .pickupDuration(PICKUP_DURATION)
                        .deliveryDuration(DELIVERY_DURATION);
                if (ridesharing) {
                    parcelBuilder = parcelBuilder
                            .deliveryTimeWindow(TimeWindow.create(pickupStartTime, deliveryStartTime + (pickupTimeWindow * 2)))
                            .neededCapacity(passenger.getAmount());
                } else {
                    parcelBuilder = parcelBuilder
                            .deliveryTimeWindow(TimeWindow.create(pickupStartTime, deliveryStartTime + (pickupTimeWindow)))
                            .neededCapacity(4);
                }
                builder.addEvent(
                        AddParcelEvent.create(parcelBuilder.buildDTO()));
//                long travelTime = (long) routingTable.getRoute(passenger.getStartPoint(), passenger.getEndPoint()).getTravelTime();
//                System.out.println("+++++++++++++++++++++++++++++++");
//                System.out.println("pickupStartTime " + pickupStartTime);
//                System.out.println("pickupTimeWindow " + pickupTimeWindow);
//                System.out.println("travelTime " + travelTime);
//                System.out.println("deliveryStartTime " + deliveryStartTime);
//
//                System.out.println("+++++++++++++++++++++++++++++++");
//                System.out.println();

            }
            totalCount++;
//            if (addedCount >= 12) {
//                break;
//            }

        }
        System.out.println(addedCount + " passengers added of the " + totalCount);
    }

    private void addPassengersDebug(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        if (!(getIoHandler().fileExists(ioHandler.getPositionedPassengersPath()))) {
            PassengerHandler pfm = new PassengerHandler(ioHandler);
            pfm.extractAndPositionPassengers();
        }
        List<SimulationObject> passengers = getIoHandler().readPositionedObjects(ioHandler.getPositionedPassengersPath());
        int totalCount = 0;
        int addedCount = 0;
        for (SimulationObject object : passengers) {
            addedCount++;
            Passenger passenger = (Passenger) object;
            long pickupStartTime = passenger.getStartTime(TAXI_DATA_START_TIME);
            long pickupTimeWindow = passenger.getStartTimeWindow(TAXI_DATA_START_TIME);
            Parcel.Builder parcelBuilder = Parcel.builder(passenger.getStartPoint(), passenger.getEndPoint())
                    .orderAnnounceTime(pickupStartTime)
                    .pickupTimeWindow(TimeWindow.create(pickupStartTime, pickupStartTime + pickupTimeWindow))
                    .pickupDuration(PICKUP_DURATION)
                    .deliveryDuration(DELIVERY_DURATION);
            if (ridesharing) {
                parcelBuilder = parcelBuilder
                        .neededCapacity(passenger.getAmount());
            } else {
                parcelBuilder = parcelBuilder
                        .neededCapacity(4);
            }
            builder.addEvent(
                    AddParcelEvent.create(parcelBuilder.buildDTO()));
            totalCount++;

            if (addedCount >= 12) {
                break;
            }

        }
        System.out.println(addedCount + " passengers added of the " + totalCount);
    }

    private long getDeliveryStartTime(Passenger passenger, RoutingTable routingTable) {
        long startTime = passenger.getStartTime(TAXI_DATA_START_TIME);
        long travelTime = (long) routingTable.getRoute(passenger.getStartPoint(), passenger.getEndPoint()).getTravelTime();
        return startTime + travelTime + PICKUP_DURATION;
    }

    private void addNYC(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        Area area = new NycArea();
        for (Point point : area.getPoints()) {
            builder.addEvent(AddDepotEvent.create(-1, point));
        }
    }

    private void addManhattan(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        Area area = new ManhattanArea();
        for (Point point : area.getPoints()) {
            builder.addEvent(AddDepotEvent.create(-1, point));
        }

    }

    private void addJFK(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        Area area = new JfkArea();
        for (Point point : area.getPoints()) {
            builder.addEvent(AddDepotEvent.create(-1, point));
        }

    }


    public static ScenarioGenerator.Builder builder() {
        return new ScenarioGenerator.Builder();
    }

    public static class Builder {
        private String taxiDataDirectory;
        private String travelTimesDirectory;
        private Date taxiDataStartTime;
        private Date taxiDataEndTime;
        private double maxVehicleSpeedKmh;
        private long pickupDuration;
        private long deliveryDuration;
        private String scenarioName;
        private int cutLength;
        private long scenarioDuration;
        private boolean traffic;
        private long tickSize;
        private boolean ridesharing;
        private boolean routingtable;
        private String routingtablePath;

        Builder() {
            taxiDataDirectory = "/media/christof/Elements/Taxi_data/";
            travelTimesDirectory = "/media/christof/Elements/Traffic_estimates/";
            taxiDataStartTime = new Date("2013-11-18 16:00:00");
            taxiDataEndTime = new Date("2013-11-18 17:00:00");
            maxVehicleSpeedKmh = 120d;
            pickupDuration = 30 * 1000L;
            deliveryDuration = 30 * 1000L;
            scenarioName = "TimeWindow";
            cutLength = 500;
            scenarioDuration = (1 * 60 * 60 * 1000L) + 1L;
            traffic = true;
            tickSize = 250L;
            ridesharing = false;
            routingtable = false;

        }

        public Builder setTaxiDataDirectory(String taxiDataDirectory) {
            this.taxiDataDirectory = taxiDataDirectory;
            return this;
        }

        public Builder setTravelTimesDirectory(String travelTimesDirectory) {
            this.travelTimesDirectory = travelTimesDirectory;
            return this;
        }

        public Builder setTaxiDataStartTime(Date taxiDataStartTime) {
            this.taxiDataStartTime = taxiDataStartTime;
            return this;
        }

        public Builder setTaxiDataEndTime(Date taxiDataEndTime) {
            this.taxiDataEndTime = taxiDataEndTime;
            return this;
        }

        public Builder setMaxVehicleSpeedKmh(double maxVehicleSpeedKmh) {
            this.maxVehicleSpeedKmh = maxVehicleSpeedKmh;
            return this;
        }

        public Builder setPickupDuration(long pickupDuration) {
            this.pickupDuration = pickupDuration;
            return this;
        }

        public Builder setDeliveryDuration(long deliveryDuration) {
            this.deliveryDuration = deliveryDuration;
            return this;
        }

        public Builder setScenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }

        public Builder setCutLength(int cutLength) {
            this.cutLength = cutLength;
            return this;
        }

        public Builder setScenarioDuration(long scenarioDuration) {
            this.scenarioDuration = scenarioDuration;
            return this;
        }

        public Builder setTraffic(boolean traffic) {
            this.traffic = traffic;
            return this;
        }

        public Builder setTickSize(long tickSize) {
            this.tickSize = tickSize;
            return this;
        }

        public Builder setRidesharing(boolean ridesharing) {
            this.ridesharing = ridesharing;
            return this;
        }


//        public Builder setRoutingtablePath(String routingtablePath) {
//            this.routingtable = true;
//            this.routingtablePath = routingtablePath;
//            return this;
//        }

        public ScenarioGenerator build() {
            return new ScenarioGenerator(this);
        }
    }







    }
