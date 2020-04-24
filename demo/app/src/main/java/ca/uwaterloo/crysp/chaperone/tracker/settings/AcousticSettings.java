package ca.uwaterloo.crysp.chaperone.tracker.settings;

public class AcousticSettings {
    // basic settings for acoustic sensing
    private int sampleRate;
    private int downSampling;
    private float soundSpeed;
    private int period;

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int newSampleRate) {
        this.sampleRate = newSampleRate;
    }

    public int getDownSampling() {
        return downSampling;
    }

    public void setDownSampling(int newDownSampling) {
        this.downSampling = newDownSampling;
    }

    public float getSoundSpeed() {
        return soundSpeed;
    }

    public void setSoundSpeed(int newSoundSpeed) {
        this.soundSpeed = newSoundSpeed;
    }

    public float getPeriod() {
        return period;
    }

    public void setPeriod(int newPeriod) {
        this.period = newPeriod;
    }

    public float getDistanceUnit() {
        return (float) downSampling * soundSpeed / sampleRate / 2;
    }

    public float getTimeUnit() {
        return (float) period / sampleRate;
    }

    public AcousticSettings() {
        // default settings
        setSampleRate(48000);
        setDownSampling(4);
        setSoundSpeed(340);
        setPeriod(2400);
    }
}
