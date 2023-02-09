import {
  BoolOption,
  ButtonOption,
  DropdownOption,
  EditableStringListOption,
  IntegerOption,
  OptionSection,
  RangeOption,
  StringInputOption,
} from "../entities/option";
import themeSelector from "@/modules/themeSelector";
import {
  deleteNamedTheme,
  getThemeNames,
  loadTheme,
  requestAllThemes,
  requestTheme,
} from "@/modules/themeManager";

//import { createHookEndpoint } from "@/modules/hooks";

//const optionChangedHook = createHookEndpoint(
//  "optionChanged",
//  (hook, ...[option, allOptions]) => {
//    hook.callback(option, allOptions);
//  }
//);

const anyOptionEquals = function (value, ...optionIdentifier) {
  return optionIdentifier.some((option) => {
    return (
      optionsModule.store.getters["options/getOption"](option).value === value
    );
  });
};

const optionsModule = {
  namespaced: true,
  store: {},
  setStore(store) {
    this.store = store;
  },
  state: () => {
    return {
      options: [
        new OptionSection({
          displayName: "General",
          name: "general",
          options: [
            new BoolOption({
              displayName: "Show asshole-points subsections",
              name: "showAhPoints",
              value: false,
            }),
          ],
        }),
        new OptionSection({
          displayName: "Ladder Settings",
          name: "ladderSettings",
          options: [
            new BoolOption({
              displayName: "Show ETA to Top/Ranker",
              name: "showETA",
              value: false,
            }),
            new BoolOption({
              displayName: "Folow own Ranker",
              name: "followOwnRanker",
              value: false,
            }),
            new DropdownOption({
              displayName: "ETA Colors",
              name: "etaColors",
              options: ["Off", "3-Color", "Gradient"],
              selectedIndex: 0,
            }),
            new BoolOption({
              displayName: "Show all rankers",
              name: "showAllRankers",
              value: false,
            }),
            new IntegerOption({
              displayName: "Rankers at top",
              name: "rankersAtTop",
              value: 5,
            }).setActiveFn(() => {
              return !optionsModule.store.getters["options/getOptionValue"](
                "showAllRankers"
              );
            }),
            new IntegerOption({
              displayName: "Rankers padding",
              name: "rankersPadding",
              value: 100,
            }).setActiveFn(() => {
              return !optionsModule.store.getters["options/getOptionValue"](
                "showAllRankers"
              );
            }),
            new BoolOption({
              displayName: "Show Bias/Multi for Rankers",
              name: "showBiasMulti",
              value: true,
            }),
            new BoolOption({
              displayName: "Show Power-Gain for Rankers",
              name: "showPowerGain",
              value: true,
            }),
            new BoolOption({
              displayName: "Hide promoted players",
              name: "hidePromotedPlayers",
              value: false,
            }),
            new BoolOption({
              displayName: "Hide vinegar and grape count",
              name: "hideVinAndGrapeCount",
              value: false,
            }),
            new BoolOption({
              displayName: "Enable Spectating Asshole Ladder ",
              name: "showSpectateAssholeLadder",
              value: false,
            }),
            new BoolOption({
              displayName: "Hide help text",
              name: "hideHelpText",
              value: false,
            }),
            new BoolOption({
              displayName: "Show promotion progress bar",
              name: "showProgressBar",
              value: false,
            }),
          ],
        }),
        new OptionSection({
          displayName: "Chat Settings",
          name: "chatSettings",
          options: [
            new EditableStringListOption({
              displayName: "Subscribed mentions",
              name: "subscribedMentions",
            }),
            new BoolOption({
              displayName: "Hide chat",
              name: "hideChat",
              value: false,
            }),
            new BoolOption({
              displayName: "Play sound on mention",
              name: "mentionSound",
              value: false,
            }),
            new BoolOption({
              displayName: "Play sound on reaching first",
              name: "reachingFirstSound",
              value: false,
            }),
            new BoolOption({
              displayName: "Play sound on promote",
              name: "promoteSound",
              value: false,
            }),
            new RangeOption({
              displayName: "Notification Volume",
              name: "notificationVolume",
              value: 50,
              min: 0,
              max: 100,
            }).setActiveFn(() =>
              anyOptionEquals(
                true,
                "mentionSound",
                "reachingFirstSound",
                "promoteSound"
              )
            ),
          ],
        }),
        new OptionSection({
          displayName: "Themes",
          name: "themes",
          options: [
            new DropdownOption({
              displayName: "Theme",
              name: "themeSelection",
              options: (() => {
                const themeNames = getThemeNames();
                //insert the default theme
                themeNames.unshift("Default");
                //capitalize the first letter of each string
                return themeNames.map((themeName) => {
                  return themeName.charAt(0).toUpperCase() + themeName.slice(1);
                });
              })(),
              callback: (ctx) => {
                themeSelector.changeTheme(ctx.get());
              },
            }),
            new StringInputOption({
              displayName: "Custom theme",
              name: "customTheme",
              callback: (val) => {
                loadTheme(val);
              },
              buttonText: "Load",
            }),
            new ButtonOption({
              displayName: "Delete current theme",
              name: "deleteCurrentTheme",
              callback: () => {
                deleteNamedTheme(themeSelector.getCurrentTheme());
                location.reload();
              },
            }),
          ],
        }),
        new OptionSection({
          displayName: "Mod Features",
          name: "modFeatures",
          options: [
            new BoolOption({
              displayName: "Enable Moderation Page",
              name: "enableModPage",
              value: false,
            }),
            new BoolOption({
              displayName: "Enable Chat Features",
              name: "enableChatModFeatures",
              value: false,
            }),
            new BoolOption({
              displayName: "Enable Ladder Features",
              name: "enableLadderModFeatures",
              value: false,
            }),
            new BoolOption({
              displayName: "Unrestricted Ladder & Chat Access",
              name: "enableUnrestrictedAccess",
              value: false,
            }),
          ],
        }).setVisibleFn(() => {
          return optionsModule.store.getters["isMod"];
        }),
      ],
    };
  },
  mutations: {
    init() {
      //TODO: load from server
    },
    loadOptions(state) {
      //TODO: load locally
      let savedOptions;
      let themes;
      try {
        savedOptions = JSON.parse(localStorage.getItem("options"));
      } catch (e) {
        localStorage.setItem("options", JSON.stringify({}));
      }
      try {
        themes = JSON.parse(localStorage.getItem("themeDatabase"));
      } catch (e) {
        localStorage.setItem("themeDatabase", JSON.stringify({}));
      }
      try {
        requestAllThemes(() => {
          try {
            if (savedOptions) {
              //get all options
              let allOptions = state.options.map(
                (section) => section.options || [section]
              );
              allOptions = [].concat(...allOptions);
              savedOptions.forEach(({ name, value }) => {
                const option = allOptions.find((o) => o.name === name);
                if (option) {
                  option.value = value;
                  if (option.name === "themeSelection") {
                    requestTheme(value);
                  }
                }
              });
              localStorage.setItem("options", JSON.stringify(savedOptions));
              localStorage.setItem("themeDatabase", JSON.stringify(themes));
            }
          } catch (e) {
            console.log(e, state);
          }
        });
      } catch (e) {
        console.error(e, state);
      }
    },
    updateOption(state, { option, payload }) {
      option.set(payload);
      //This is always guaranteed to be the new value
      //payload may also include other properties that are describing other aspects of the option
      //eslint-disable-next-line no-unused-vars
      const newValue = payload.value;

      //Saving to localstorage
      let allOptions = state.options.map((section) => section.options);
      allOptions = [].concat(...allOptions);
      let optionNamesAndValues = allOptions
        .map((option) => {
          return {
            name: option.name,
            value: option.value,
          };
        })
        .filter((o) => o.value || o.value === false);
      localStorage.setItem("options", JSON.stringify(optionNamesAndValues));

      //Now updating the option's display properties

      //TODO: save to server

      //Call hooks to let users know that the option has changed
      //optionChangedHook(option, state.options);
    },
  },
  actions: {},
  getters: {
    getOption: (state) => (name) => {
      for (const section of state.options) {
        if (section.name === name) {
          return section;
        }
        if (!(section instanceof OptionSection)) {
          continue;
        }
        for (const option of section.options) {
          if (option.name === name) {
            return option;
          }
        }
      }
    },
    getOptionValue: (state, getters) => (name) => {
      const option = getters.getOption(name);
      if (option) {
        return option.get();
      }
      return null;
    },
  },
};

export default optionsModule;
