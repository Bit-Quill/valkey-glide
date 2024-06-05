package glide.api.models.commands;

public class SortOptions {
    public interface SortCommandOptions {
        String[] toArgs();
    }

    public interface SortReadOnlyOptions extends SortCommandOptions{

    }

    public interface SortSupportedStandaloneOptions extends SortCommandOptions{

    }


}
