package com.corvid.bes.util;

import com.corvid.genericdto.data.gdto.GenericDTO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by kodero on 3/25/16.
 */
@ApplicationScoped
@Path("/dateOptions")
public class DateOptionsResource {

    @GET
    public Response getList(){
        List<GenericDTO> dateOptions = new ArrayList<>();
        DateTime currentDate = new DateTime();
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");

        //current
        dateOptions.add(new GenericDTO("dateOption").addString("code", "today").addString("label", "Today").addString("from", df.format(new Date())).addString("to", df.format(new Date())).addString("group", "current"));
        dateOptions.add(new GenericDTO("dateOption").addString("code", "this_week").addString("label", "This Week").addString("from", df.format(new LocalDate().withDayOfWeek(DateTimeConstants.MONDAY).toDate())).addString("to", df.format(new Date())).addString("group", "current"));
        dateOptions.add(new GenericDTO("dateOption").addString("code", "this_month").addString("label", "This Month").addString("from", df.format(new LocalDate().withDayOfMonth(1).toDate())).addString("to", df.format(new Date())).addString("group", "current"));
        dateOptions.add(new GenericDTO("dateOption").addString("code", "this_quarter").addString("label", "This Quarter").addString("from", df.format(getStartOfQuarter(currentDate.toDate()))).addString("to", df.format(new LocalDate().dayOfMonth().withMaximumValue().toDate())).addString("group", "current"));
        dateOptions.add(new GenericDTO("dateOption").addString("code", "year_to_date").addString("label", "Year to Date").addString("from", df.format(currentDate.withMonthOfYear(1).withDayOfMonth(1).toDate())).addString("to", df.format(currentDate.toDate())).addString("group", "current"));

        //previous
        //current
        DateTime startOfPrevQuarter = getStartOfPrevQuarter(new Date());
        dateOptions.add(new GenericDTO("dateOption").addString("code", "yesterday").addString("label", "Yesterday").addString("from", df.format(currentDate.minusDays(1).toDate())).addString("to", df.format(currentDate.minusDays(1).toDate())).addString("group", "previous"));
        dateOptions.add(new GenericDTO("dateOption").addString("code", "last_week").addString("label", "Last Week").addString("from", df.format(new LocalDate().minusDays(7).withDayOfWeek(DateTimeConstants.MONDAY).toDate())).addString("to", df.format(new LocalDate().minusDays(7).withDayOfWeek(DateTimeConstants.SUNDAY).toDate())).addString("group", "previous"));
        dateOptions.add(new GenericDTO("dateOption").addString("code", "last_month").addString("label", "Last Month").addString("from", df.format(new LocalDate().minusMonths(1).withDayOfMonth(1).toDate())).addString("to", df.format(new LocalDate().minusMonths(1).dayOfMonth().withMaximumValue().toDate())).addString("group", "previous"));
        dateOptions.add(new GenericDTO("dateOption").addString("code", "last_quarter").addString("label", "Last Quarter").addString("from", df.format(startOfPrevQuarter.toDate())).addString("to", df.format(startOfPrevQuarter.plusMonths(2).toDate())).addString("group", "previous"));
        dateOptions.add(new GenericDTO("dateOption").addString("code", "last_year").addString("label", "Last Year").addString("from", df.format(currentDate.minusYears(1).withMonthOfYear(1).withDayOfMonth(1).toDate())).addString("to", df.format(new LocalDate().minusYears(1).withMonthOfYear(12).dayOfMonth().withMaximumValue().toDate())).addString("group", "previous"));
        return Response.ok(dateOptions).build();
    }

    private DateTime getStartOfPrevQuarter(Date date) {
        DateTime dt = new DateTime(date);
        int month = dt.getMonthOfYear();
        int prevStartMonth = 1;//default is first quarter
        if(month > 0 && month <= 3) prevStartMonth = -10;
        else if(month > 3 && month <= 6) prevStartMonth = 1;
        else if(month > 6 && month <= 9) prevStartMonth = 4;
        else if(month > 9 && month <= 12) prevStartMonth = 7;
        if(prevStartMonth < 0) {
            return dt.minusYears(1).withMonthOfYear(Math.abs(prevStartMonth)).withDayOfMonth(1);
        }
        return dt.withMonthOfYear(Math.abs(prevStartMonth)).withDayOfMonth(1);
    }

    private Date getStartOfQuarter(Date date) {
        DateTime dt = new DateTime(date);
        int month = dt.getMonthOfYear();
        int startMonth = 1;//default is first quarter
        if(month > 3 && month <= 6) startMonth = 4;
        else if(month > 6 && month <= 9) startMonth = 7;
        else if(month > 9 && month <= 12) startMonth = 10;
        return dt.withMonthOfYear(startMonth).withDayOfMonth(1).toDate();
    }
}
