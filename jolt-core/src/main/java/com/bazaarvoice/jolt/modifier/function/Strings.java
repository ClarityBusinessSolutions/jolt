/*
 * Copyright 2013 Bazaarvoice, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bazaarvoice.jolt.modifier.function;

import com.bazaarvoice.jolt.common.Optional;
import com.bazaarvoice.jolt.exception.SpecException;
import com.bazaarvoice.jolt.utils.StringTools;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings( "deprecated" )
public class Strings {

    public static final class toLowerCase extends Function.SingleFunction<String> {
        @Override
        protected Optional<String> applySingle( final Object arg ) {

            if ( ! (arg instanceof String) ) {
                return Optional.empty();
            }

            String argString = (String) arg;

            return Optional.of( argString.toLowerCase() );
        }
    }

    public static final class toUpperCase extends Function.SingleFunction<String> {
        @Override
        protected Optional<String> applySingle( final Object arg ) {

            if ( ! (arg instanceof String) ) {
                return Optional.empty();
            }

            String argString = (String) arg;

            return Optional.of( argString.toUpperCase() );
        }
    }

    public static final class trim extends Function.SingleFunction<String> {
        @Override
        protected Optional<String> applySingle( final Object arg ) {

            if ( ! (arg instanceof String) ) {
                return Optional.empty();
            }

            String argString = (String) arg;

            return Optional.of( argString.trim() );
        }
    }

    public static final class concat extends Function.ListFunction {
        @Override
        protected Optional<Object> applyList( final List<Object> argList ) {
            StringBuilder sb = new StringBuilder(  );
            for(Object arg: argList ) {
                if ( arg != null ) {
                    sb.append(arg.toString() );
                }
            }
            return Optional.of(sb.toString());
        }
    }

    public static final class substring extends Function.ListFunction {

        @Override
        protected Optional<Object> applyList(List<Object> argList) {

            // There is only one path that leads to success and many
            //  ways for this to fail.   So using a do/while loop
            //  to make the bailing easy.
            do {

                // if argList is null or not the right size; bail
                if(argList == null || argList.size() != 3 ) {
                    break;
                }

                if ( ! ( argList.get(0) instanceof String &&
                         argList.get(1) instanceof Integer &&
                         argList.get(2) instanceof Integer ) ) {
                    break;
                }

                // If we get here, then all these casts should work.
                String tuna = (String) argList.get(0);
                int start = (Integer) argList.get(1);
                int end = (Integer) argList.get(2);

                // do start and end make sense?
                if ( start >= end || start < 0 || end < 1 || end > tuna.length() ) {
                    break;
                }

                return Optional.of(tuna.substring(start, end));

            } while( false );

            // if we got here, then return an Optional.empty.
            return Optional.empty();
        }
    }

    @SuppressWarnings( "unchecked" )
    public static final class join extends Function.ArgDrivenListFunction<String> {

        @Override
        protected Optional<Object> applyList( final String specialArg, final List<Object> args ) {
            StringBuilder sb = new StringBuilder(  );
            for(int i=0; i < args.size(); i++) {
                Object arg = args.get(i);
                if (arg != null ) {
                    String argString = arg.toString();
                    if( !("".equals( argString ))) {
                        sb.append( argString );
                        if ( i < args.size() - 1 ) {
                            sb.append( specialArg );
                        }
                    }
                }
            }
            return Optional.of( sb.toString() );
        }
    }

    public static final class split extends Function.ArgDrivenSingleFunction<String, List> {
      @Override
      protected Optional<List> applySingle(final String separator, final Object source) {
        if (source == null || separator == null) {
          return Optional.empty();
        }
        else if ( source instanceof String ) {
          // only try to split input strings
          String inputString = (String) source;
          return Optional.of( Arrays.asList(inputString.split(separator)) );
        }
        else {
          return Optional.empty();
        }
      }
    }


    public static final class leftPad extends Function.ArgDrivenListFunction<String> {
        @Override
        protected Optional<Object> applyList(String source, List<Object> args) {

            return padString( true, source, args );
        }
    }

    public static final class rightPad extends Function.ArgDrivenListFunction<String> {
        @Override
        protected Optional<Object> applyList(String source, List<Object> args) {

            return padString( false, source, args );
        }
    }

    private static Optional<Object> padString( boolean leftPad, String source, List<Object> args ) {

        // There is only one path that leads to success and many
        //  ways for this to fail.   So using a do/while loop
        //  to make the bailing easy.
        do {

            if(source == null || args == null ) {
                break;
            }

            if ( ! ( args.get(0) instanceof Integer &&
                     args.get(1) instanceof String ) ) {
                break;
            }

            Integer width = (Integer) args.get(0);

            // if the width param is stupid; bail
            if ( width <= 0 || width > 500 ) {
                break;
            }

            String filler = (String) args.get(1);

            // filler can only be a single char
            //  otherwise the math becomes hard
            if ( filler.length() != 1 ) {
                break;
            }

            char fillerChar = filler.charAt( 0 );

            // if the desired width of the overall padding is smaller than
            //  the source string, then just return the source string.
            if( width <= source.length() ) {
                return Optional.of( source );
            }

            int padLength = width - source.length();
            char[] padArray = new char[padLength];

            Arrays.fill( padArray, fillerChar );

            StringBuilder sb = new StringBuilder();

            if ( leftPad ) {
                sb.append( padArray ).append( source );
            }
            else {
                sb.append( source ).append( padArray );
            }

            return Optional.of( sb.toString() );

        } while ( false );

        return Optional.empty();
    }

    public static final class transformDate extends Function.ArgDrivenListFunction<String> {
        @Override
        protected Optional<Object> applyList(final String originalDate, final List<Object> args) {

            if (!(args.get(0) instanceof String &&
                    args.get(1) instanceof String)) {
                //TODO should we warn user that we were unable to parse the input or output pattern?
                return Optional.of(originalDate);
            }

            String inputPatterns = (String) args.get(0);
            String outputPattern = (String) args.get(1);

            String originalTimezone = "UTC";
            String newTimezone = "";

            if (args.size() == 3) {
                originalTimezone = (String) args.get(2);
            }

            if (args.size() == 4) {
                originalTimezone = (String) args.get(2);
                newTimezone = (String) args.get(3);
            }

            ZonedDateTime zonedDateTime;
            DateTimeFormatter originalFormatter;
            DateTimeFormatter outputFormatter;

            ZoneId originalZoneId = ZoneOffset.UTC;

            if (StringTools.isNotBlank(originalTimezone)) {

                    originalZoneId = ZoneId.of(originalTimezone);
            }

            try {

                if (inputPatterns.equalsIgnoreCase("EPOCH_MILLI")) {

                    Long epochTime = Long.parseLong(originalDate);

                    //zonedDateTime = LocalDateTime.ofEpochSecond(epochTime, 0, ZoneOffset.UTC);
                    zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochTime), originalZoneId);


                } else if (inputPatterns.equalsIgnoreCase("EPOCH_SECOND")) {

                    Long epochTime = Long.parseLong(originalDate);

                    //zonedDateTime = LocalDateTime.ofEpochSecond(epochTime, 0, ZoneOffset.UTC);
                    zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochTime), originalZoneId);


                } else {
                    originalFormatter = DateTimeFormatter.ofPattern(inputPatterns);//.withZone(zoneId);

                    LocalDateTime ldt = LocalDateTime.parse(originalDate, originalFormatter);
                    zonedDateTime = ZonedDateTime.of(ldt, originalZoneId);

                    if (StringTools.isNotBlank(newTimezone)) {

                        ZoneId newZoneId = ZoneId.of(newTimezone);
                        zonedDateTime = zonedDateTime.withZoneSameInstant(newZoneId);
                    }
                }

                outputFormatter = DateTimeFormatter.ofPattern(outputPattern);

            } catch (Exception e) {
                return Optional.of("ERROR: " + e.getMessage());
            }

            //System.out.println("Output formatter: "+ outputPattern + " "+outputFormatter);
            System.out.format("Formatted date time with default zone name is %s\n", zonedDateTime.format(outputFormatter));

            return Optional.of(zonedDateTime.format(outputFormatter));
        }
    }
}