package ru.tsc.crm.mapping;


import io.quarkus.scheduler.Scheduled;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class MappingRefresh {

    //@Scheduled(cron = "{refresh.cron.expr}")
    public void refresh() {
        doRefresh();
    }

    //TODO ходить в базу и заполнять мапу
    private void doRefresh() {
        System.out.println(LocalDateTime.now() + " Refreshing");
    }
}
