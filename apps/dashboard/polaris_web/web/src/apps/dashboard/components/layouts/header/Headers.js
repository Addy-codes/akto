import {TopBar, Icon, Text, ActionList} from '@shopify/polaris';
import {NotificationMajor} from '@shopify/polaris-icons';
import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Store from '../../../store';
import PersistStore from '../../../../main/PersistStore';
import './Headers.css'
import api from '../../../../signup/api';
import func from '@/util/func';

export default function Header() {
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
    const [isSecondaryMenuOpen, setIsSecondaryMenuOpen] = useState(false);
    const [isSearchActive, setIsSearchActive] = useState(false);
    const [searchValue, setSearchValue] = useState('');
    
    const navigate = useNavigate()

    const setLeftNavSelected = Store((state) => state.setLeftNavSelected)
    const leftNavCollapsed = Store((state) => state.leftNavCollapsed)
    const toggleLeftNavCollapsed = Store(state => state.toggleLeftNavCollapsed)
    const username = Store((state) => state.username)
    const storeAccessToken = PersistStore(state => state.storeAccessToken)

    const allRoutes = Store((state) => state.allRoutes)
    const allCollections = Store((state) => state.allCollections)
    const searchItemsArr = func.getSearchItemsArr(allRoutes,allCollections)

    const handleLeftNavCollapse = () => {
        if (!leftNavCollapsed) {
            setLeftNavSelected('')
        }

        toggleLeftNavCollapsed()
    }

    const toggleIsUserMenuOpen = useCallback(
        () => setIsUserMenuOpen((isUserMenuOpen) => !isUserMenuOpen),
        [],
      );
    
    const toggleIsSecondaryMenuOpen = useCallback(
        () => setIsSecondaryMenuOpen((isSecondaryMenuOpen) => !isSecondaryMenuOpen),
        [],
    );

    const handleLogOut = async () => {
        storeAccessToken(null)
        await api.logout()
        navigate("/login")  
    }

    const handleSwitchUI = async () => {
        await api.updateAktoUIMode({ aktoUIMode: "VERSION_1" })
        window.location.reload()
    }

    const userMenuMarkup = (
        <TopBar.UserMenu
            actions={[
                {
                    items: [
                        {id: "manage", content: 'Manage Account'}, 
                        {id: "log-out", content: 'Log out', onAction: handleLogOut}
                    ],
                },
                {
                    items: [
                        {id: "switch-ui", content: 'Switch to legacy', onAction: handleSwitchUI}, 
                        {content: 'Documentation', onAction: ()=>{window.open("https://docs.akto.io/readme")}},
                        {content: 'Tutorials', onAction: ()=>{window.open("https://www.youtube.com/@aktodotio")}},
                        {content: 'Changelog', onAction: ()=>{window.open("https://app.getbeamer.com/akto/en")}},
                        {content: 'Discord Support', onAction: ()=>{window.open("https://discord.com/invite/Wpc6xVME4s")}},
                        {content: 'Star On Github', onAction: ()=>{window.open("https://github.com/akto-api-security/akto")}}
                    ],
                },
            ]}
            initials={func.initials(username)}
            open={isUserMenuOpen}
            onToggle={toggleIsUserMenuOpen}
        />
    );

    const handleSearchResultsDismiss = useCallback(() => {
        setIsSearchActive(false);
        setSearchValue('');
    }, []);

    const handleSearchChange = useCallback((value) => {
        setSearchValue(value);
        setIsSearchActive(value.length > 0);
    }, []);

    const handleNavigateSearch = (url) =>{
        navigate(url)
        handleSearchResultsDismiss()
    }

    const searchItems = searchItemsArr.map((item)=>{
        return{
            content: item.content,
            onAction: ()=> handleNavigateSearch(item.url),
        }
    })

    const searchResultsMarkup = (
        <ActionList
            items={searchItems.filter(x => x.content.toLowerCase().includes(searchValue.toLowerCase()))}
        />
    );

    const searchFieldMarkup = (
        <TopBar.SearchField
            placeholder="Search for pages and API collections"
            showFocusBorder
            onChange={handleSearchChange}
            value={searchValue}
        />
    );

    const secondaryMenuMarkup = (
        <TopBar.Menu 
            activatorContent={
                <span>
                <Icon source={NotificationMajor}/>
                <Text as="span" visuallyHidden>
                    Secondary menu
                </Text>
                </span>
            }
            open={isSecondaryMenuOpen}
            onOpen={toggleIsSecondaryMenuOpen}
            onClose={toggleIsSecondaryMenuOpen}
            actions={[
                {
                    items: [{
                        prefix: <div style={{marginLeft: '14px'}} id='beamer-btn'>Updates</div>
                    }],
                },
            ]}
        />
    );

    const topBarMarkup = (
        <div className='topbar'>
            <TopBar
                showNavigationToggle
                userMenu={userMenuMarkup}
                secondaryMenu={secondaryMenuMarkup}
                searchField={searchFieldMarkup}
                searchResultsVisible={isSearchActive}
                searchResults={searchResultsMarkup}
                onSearchResultsDismiss={handleSearchResultsDismiss}
            />
        </div>
    );

    return (
        topBarMarkup
    );
}