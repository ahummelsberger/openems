export const TRANSLATION = {
    General: {
        Cumulative: "Cumulatieve Waarden",
        Grid: "Net",
        GridBuy: "Netafname",
        GridSell: "Netteruglevering",
        OffGrid: "Geen Netaansluiting!",
        Production: "Opwekking",
        Consumption: "Verbruik",
        Load: "Laden",
        Power: "Vermogen",
        StorageSystem: "Batterij",
        History: "Historie",
        Live: 'Live',
        NoValue: "Geen waarde",
        Soc: "Laadstatus",
        Percentage: "Procent",
        More: "Meer…",
        ChargePower: "Laad vermogen",
        DischargePower: "Ontlaad vermogen",
        ActualPower: "e-car Laad vermogen",
        PeriodFromTo: "van {{value1}} tot {{value2}}", // value1 = beginning date, value2 = end date
        DateFormat: "dd-MM-yyyy", // e.g. German: dd.MM.yyyy, English: yyyy-MM-dd (dd = Day, MM = Month, yyyy = Year)
        Week: {
            Monday: "Maandag",
            Tuesday: "Dinsdag",
            Wednesday: "Woensdag",
            Thursday: "Donderdag",
            Friday: "Vrijdag",
            Saturday: "Zaterdag",
            Sunday: "Zondag"
        },
        ReportValue: "Rapporteer beschadigde gegevens"
    },
    Menu: {
        Index: "Overzicht",
        AboutUI: "Over OpenEMS UI",
        GeneralSettings: 'Algemene instellingen',
        EdgeSettings: 'FEMS instellingen',
        Menu: 'Menu',
        Overview: 'FEMS overzicht',
        Logout: 'Uitloggen'
    },
    Index: {
        AllConnected: "Alle verbindingen gemaakt.",
        ConnectionSuccessful: "Succesvol verbonden met {{value }}.", // (value = Name vom Websocket)
        ConnectionFailed: "Verbinding met {{ value } } mislukt.", // (value = Name vom Websocket)
        ToEnergymonitor: "Naar Energiemonitor...",
        IsOffline: "OpenEMS is offline!"
    },
    Edge: {
        Index: {
            Energymonitor: {
                Title: "Energiemonitor",
                ConsumptionWarning: "Verbruik & onbekende producenten",
                Storage: "Batterij",
                ReactivePower: "Blind vermogen",
                ActivePower: "Actief vermogen",
                GridMeter: "Energiemeter",
                ProductionMeter: "Productiemeter"
            },
            Energytable: {
                Title: "Energie tabel",
                LoadingDC: "DC laden",
                ProductionDC: "Generatie DC"
            },
            Widgets: {
                CHP: {
                    LowThreshold: "Lage drempelwaarde",
                    HighThreshold: "hoge drempel"
                },
                EVCS: {
                    ChargingStation: "Laadstation",
                    Status: "Staat",
                    Starting: "Beginnend",
                    NotReadyForCharging: "Niet klaar voor opladen",
                    ReadyForCharging: "Klaar om op te laden",
                    Charging: "Is aan het laden",
                    Error: "Fout",
                    NotAuthorized: "Geen bevoegdheid",
                    Unplugged: "Unplugged",
                    CharingStationPluggedIn: "Laadstation aangesloten",
                    ChargingStationPluggedInLocked: "Laadstation aangesloten + op slot",
                    ChargingStationPluggedInEV: "Laadstation + E-Auto aangesloten",
                    ChargingStationPluggedInEVLocked: "Laadstation + E-Auto aangesloten + op slot",
                    ChargingLimit: "Laadlimiet",
                    ChargingPower: "Oplaadvermogen",
                    CurrentCharge: "Huidige lading",
                    TotalCharge: "Totale lading",
                    EnforceCharging: "Handhaaf het laden",
                    Cable: "Kabel",
                    CableNotConnected: "Kabel is niet aangesloten",
                    CarFull: "Auto is vol",
                    EnergieSinceBeginning: "Energie sinds het begin van de lading",
                    ChargeMode: "laadmodus",
                    ActivateCharging: "Activeer het laadstation",
                    NoConnection: {
                        Description: "Hij kon niet op het laadstation worden aangesloten.",
                        Help1: "Controleer of het laadstation is ingeschakeld en via het netwerk kan worden bereikt",
                        Help1_1: "Het IP-adres van het laadstation verschijnt bij het opnieuw inschakelen"
                    },
                    OptimizedChargeMode: {
                        Name: "Geoptimaliseerd laden",
                        ShortName: "geoptimaliseerde",
                        Info: "In deze modus wordt de belasting van de auto aangepast aan de huidige productie en het huidige verbruik.",
                        MinInfo: "Als u wilt voorkomen dat de auto 's nachts niet oplaadt, kunt u een minimale lading instellen.",
                        MinCharging: "Minimale vergoeding betalen?",
                        ChargingPriority: "Afhankelijk van de prioriteit, wordt het geselecteerde onderdeel eerst geladen"
                    },
                    ForceChargeMode: {
                        Name: "Gedwongen laden",
                        ShortName: "Gedwongen",
                        Info: "In deze modus wordt het laden van de auto afgedwongen, d.w.z. het is altijd gegarandeerd dat de auto wordt opgeladen, zelfs als het laadstation toegang moet hebben tot netstroom.",
                        MaxCharging: "Maximale laadstroom",
                        MaxChargingDetails: "Als de auto de ingevoerde maximale waarde niet kan laden, wordt het vermogen automatisch beperkt."
                    }
                }
            }
        },
        History: {
            SelectedPeriod: "Geselecteerde periode: ",
            OtherPeriod: "Andere periode",
            Period: "Periode",
            Today: "Vandaag",
            Yesterday: "Gisteren",
            LastWeek: "Vorige week",
            LastMonth: "Vorige maand",
            LastYear: "Vorig jaar",
            Go: "Ga!"
        },
        Config: {
            Index: {
                Bridge: "Verbindingen en apparaten",
                Scheduler: "Toepassingsschema",
                Controller: "Toepassingen",
                Simulator: "Simulator",
                ExecuteSimulator: "Simulatie uitvoeren",
                Log: "Log",
                LiveLog: "Live System log",
                AddComponents: "Componenten installeren",
                AdjustComponents: "Componenten configureren",
                ManualControl: "Handmatige bediening",
                DataStorage: "Gegevensopslag"
            },
            More: {
                ManualCommand: "Handmatig commando",
                Send: "Verstuur",
                RefuInverter: "REFU inverter",
                RefuStartStop: "Inverter starten/ stoppen",
                RefuStart: "Start",
                RefuStop: "Stop",
                ManualpqPowerSpecification: "Gespecificeerd vermogen",
                ManualpqSubmit: "Toepassen",
                ManualpqReset: "Reset"
            },
            Scheduler: {
                NewScheduler: "New Schema...",
                Class: "Soort: ",
                NotImplemented: "Gegevens niet geïmplementeerd: ",
                Contact: "Dit zou niet mogen gebeuren. Neem contact op met <a href=\"mailto:{{value}}\">{{value}}</a>.",
                Always: "Altijd"
            },
            Log: {
                AutomaticUpdating: "Automatisch updaten",
                Timestamp: "Tijdspit",
                Level: "Niveau",
                Source: "Bron",
                Message: "Bericht"
            },
            Controller: {
                InternallyID: "Intern ID:",
                App: "App:",
                Priority: "Prioriteit: "
            },
            Bridge: {
                NewDevice: "Nieuw apparaat…",
                NewConnection: "Nieuwe verbinding..."
            }
        }
    },
    About: {
        UI: "Gebruikersinterface voor OpenEMS",
        Developed: "Deze gebruikersinterface is ontwikkeld als open-source-software.",
        Sourcecode: "Broncode",
        CurrentDevelopments: "Huidige ontwikkelingen",
        Build: "Versie",
        Contact: "Voor meer informatie of suggesties over het systeem, neem contact op met het team via <a href=\"mailto:{{value}}\">{{value}}</a>.",
        Language: "Selecteer taal: "
    },
    Notifications: {
        Failed: "Verbinding mislukt.",
        LoggedInAs: "Aangemeld als gebruiker {{ value } }.", // (value = Benutzername)
        LoggedIn: "Aangemeld.",
        AuthenticationFailed: "Geen verbinding.Autorisatie mislukt.",
        Closed: "Verbinding beëindigd."
    }
}