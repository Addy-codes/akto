import { Button, Collapsible, Divider, LegacyCard, LegacyStack, Text } from "@shopify/polaris"
import { ChevronRightMinor, ChevronDownMinor } from '@shopify/polaris-icons';
import { useState } from "react";
import api from "../api"
import { useEffect } from "react";
import HardCoded from "./HardCoded";
import SpinnerCentered from "../../../components/progress/SpinnerCentered";
import TestingStore from "../testingStore";
import Automated from "./Automated";
import Store from "../../../store";
import PageWithMultipleCards from "../../../components/layouts/PageWithMultipleCards"

const headers = [
    {
        title: "Config name",
        text: "Config name",
        value: "configName",
    },
    {
        title: "Status",
        text: "Status",
        value: "Status",
    },
    {   
        title: 'Values',
        text: 'Values', 
        value: 'values',
    },
    {
        title: 'Impacting categories', 
        text: 'Impacting categories', 
        value: 'impactingCategories',
    }
]


function UserConfig() {

    const setToastConfig = Store(state => state.setToastConfig)
    const setAuthMechanism = TestingStore(state => state.setAuthMechanism)
    const [isLoading, setIsLoading] = useState(true)
    const [hardcodedOpen, setHardcodedOpen] = useState(true);

    const handleToggleHardcodedOpen = () => setHardcodedOpen((prev) => !prev)

    async function fetchAuthMechanismData() {
        setIsLoading(true)
        const authMechanismDataResponse = await api.fetchAuthMechanismData()
        if (authMechanismDataResponse && authMechanismDataResponse.authMechanism) {
            const authMechanism = authMechanismDataResponse.authMechanism
            setAuthMechanism(authMechanism)
            if (authMechanism.type === "HARDCODED") setHardcodedOpen(true)
            else setHardcodedOpen(false)
        }
        setIsLoading(false)
    }

    useEffect(() => {
        fetchAuthMechanismData()
    }, [])

    async function handleStopAlltests() {
        await api.stopAllTests()
        setToastConfig({ isActive: true, isError: false, message: "All tests stopped!" })
    }

    const bodyComponent = (
        <LegacyCard sectioned title="Choose auth token configuration" key="bodyComponent">
            <Divider />
            <LegacyCard.Section>
                <LegacyStack vertical>
                    <Button
                        id={"hardcoded-token-expand-button"}
                        onClick={handleToggleHardcodedOpen}
                        ariaExpanded={hardcodedOpen}
                        icon={hardcodedOpen ? ChevronDownMinor : ChevronRightMinor}
                        ariaControls="hardcoded"
                    >
                        Hard coded
                    </Button>
                    <Collapsible
                        open={hardcodedOpen}
                        id="hardcoded"
                        transition={{ duration: '500ms', timingFunction: 'ease-in-out' }}
                        expandOnPrint
                    >
                        <HardCoded />
                    </Collapsible>
                </LegacyStack>
            </LegacyCard.Section>


            <LegacyCard.Section>
                <LegacyStack vertical>
                    <Button
                        id={"automated-token-expand-button"}
                        onClick={handleToggleHardcodedOpen}
                        ariaExpanded={!hardcodedOpen}
                        icon={!hardcodedOpen ? ChevronDownMinor : ChevronRightMinor}
                        ariaControls="automated"
                    >
                        Automated
                    </Button>
                    <Collapsible
                        open={!hardcodedOpen}
                        id="automated"
                        transition={{ duration: '500ms', timingFunction: 'ease-in-out' }}
                        expandOnPrint
                    >
                        <Automated /> 
                    </Collapsible>
                </LegacyStack>
            </LegacyCard.Section>

        </LegacyCard>
    )

    const components = [bodyComponent]

    return (
        isLoading ? <SpinnerCentered /> 
           :<PageWithMultipleCards 
                components={components}
                isFirstPage={true}
                divider={true}
                title ={
                    <Text variant="headingLg">
                        User config
                    </Text>
                }
                primaryAction={{ content: 'Stop all tests', onAction: handleStopAlltests }}
            />

    )
}

export default UserConfig