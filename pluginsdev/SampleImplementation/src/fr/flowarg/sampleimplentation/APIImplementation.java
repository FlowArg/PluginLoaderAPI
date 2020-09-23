package fr.flowarg.sampleimplentation;

import fr.flowarg.pluginloaderapi.api.IAPI;

import java.util.UUID;

public class APIImplementation implements IAPI
{
    private final String randomUUID;

    APIImplementation()
    {
        this.randomUUID = UUID.randomUUID().toString();
    }

    @Override
    public String getAPIName()
    {
        return "APIImplementation";
    }

    public String getRandomUUID()
    {
        return this.randomUUID;
    }
}
